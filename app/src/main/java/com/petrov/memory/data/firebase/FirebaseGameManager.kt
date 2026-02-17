package com.petrov.memory.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.petrov.memory.domain.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Менеджер Firebase для онлайн-игры
 * Управляет комнатами, ходами и синхронизацией между игроками
 */
class FirebaseGameManager {

    private val database: DatabaseReference = FirebaseDatabase.getInstance(
        "https://memorygame-f92f8-default-rtdb.europe-west1.firebasedatabase.app"
    ).reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    private val roomsRef = database.child("rooms")
    private val movesRef = database.child("moves")
    
    /**
     * Получить ID текущего игрока (анонимная аутентификация)
     */
    suspend fun getCurrentPlayerId(): String {
        android.util.Log.d("FirebaseManager", "getCurrentPlayerId: Starting...")
        
        val currentUser = auth.currentUser
        return if (currentUser != null) {
            android.util.Log.d("FirebaseManager", "getCurrentPlayerId: User already signed in: ${currentUser.uid}")
            currentUser.uid
        } else {
            android.util.Log.d("FirebaseManager", "getCurrentPlayerId: Signing in anonymously...")
            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: throw IllegalStateException("Failed to authenticate")
            android.util.Log.d("FirebaseManager", "getCurrentPlayerId: Signed in successfully: $uid")
            uid
        }
    }
    
    /**
     * Создать новую игровую комнату
     */
    suspend fun createRoom(level: Int, timerMode: TimerMode, timeLimit: Int?): String {
        android.util.Log.d("FirebaseManager", "createRoom: Starting...")
        
        val playerId = getCurrentPlayerId()
        android.util.Log.d("FirebaseManager", "createRoom: Player ID = $playerId")
        
        val roomId = roomsRef.push().key ?: throw IllegalStateException("Failed to create room")
        android.util.Log.d("FirebaseManager", "createRoom: Room ID = $roomId")
        
        val cards = generateCards(level)
        android.util.Log.d("FirebaseManager", "createRoom: Generated ${cards.size} cards")
        
        val room = OnlineGameRoom(
            roomId = roomId,
            hostPlayerId = playerId,
            level = level,
            timerMode = timerMode.name,
            timeLimit = timeLimit,
            currentPlayerId = playerId,
            cards = cards
        )
        
        android.util.Log.d("FirebaseManager", "createRoom: Saving to Firebase...")
        roomsRef.child(roomId).setValue(room.toMap()).await()
        android.util.Log.d("FirebaseManager", "createRoom: Saved successfully!")
        
        return roomId
    }
    
    /**
     * Присоединиться к существующей комнате
     */
    suspend fun joinRoom(roomId: String): Boolean {
        val playerId = getCurrentPlayerId()
        val snapshot = roomsRef.child(roomId).get().await()
        
        if (!snapshot.exists()) return false
        
        val room = snapshot.getValue(OnlineGameRoom::class.java) ?: return false

        if (room.guestPlayerId != null) return false

        roomsRef.child(roomId).child("guestPlayerId").setValue(playerId).await()
        return true
    }
    
    /**
     * Начать игру (доступно только хосту)
     */
    suspend fun startGame(roomId: String): Boolean {
        val playerId = getCurrentPlayerId()
        val snapshot = roomsRef.child(roomId).get().await()
        
        if (!snapshot.exists()) return false
        
        val room = snapshot.getValue(OnlineGameRoom::class.java) ?: return false

        if (room.hostPlayerId != playerId) return false

        if (room.guestPlayerId == null) return false
        
        roomsRef.child(roomId).child("gameStarted").setValue(true).await()
        return true
    }
    
    /**
     * Сделать ход (открыть карточку)
     */
    suspend fun makeMove(roomId: String, cardId: Int): Boolean = suspendCoroutine { continuation ->
        android.util.Log.d("FirebaseManager", "makeMove: roomId=$roomId, cardId=$cardId")
        
        val playerId = try {
            auth.currentUser?.uid ?: throw IllegalStateException("Not authenticated")
        } catch (e: Exception) {
            continuation.resumeWithException(e)
            return@suspendCoroutine
        }

        var success = false
        
        roomsRef.child(roomId).runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                val room = currentData.getValue(OnlineGameRoom::class.java)
                    ?: return Transaction.abort()

                if (room.currentPlayerId != playerId) {
                    android.util.Log.w("FirebaseManager", "makeMove(txn): Not your turn")
                    return Transaction.abort()
                }

                if (!room.gameStarted || room.gameFinished) {
                    android.util.Log.w("FirebaseManager", "makeMove(txn): Game not active")
                    return Transaction.abort()
                }

                if (room.checkingMatch) {
                    android.util.Log.w("FirebaseManager", "makeMove(txn): Match check in progress")
                    return Transaction.abort()
                }

                if (room.firstFlippedCardId != null && room.secondFlippedCardId != null) {
                    android.util.Log.w("FirebaseManager", "makeMove(txn): Already 2 cards flipped")
                    return Transaction.abort()
                }

                val cardIndex = room.cards.indexOfFirst { it.id == cardId }
                if (cardIndex == -1) {
                    android.util.Log.e("FirebaseManager", "makeMove(txn): Card not found")
                    return Transaction.abort()
                }
                
                val card = room.cards[cardIndex]
                
                if (card.matched || card.flipped) {
                    android.util.Log.w("FirebaseManager", "makeMove(txn): Card already flipped")
                    return Transaction.abort()
                }

                val updatedCards = room.cards.toMutableList()
                updatedCards[cardIndex] = card.copy(flipped = true)
                currentData.child("cards").value = updatedCards.map { it.toMap() }

                if (room.firstFlippedCardId == null) {
                    android.util.Log.d("FirebaseManager", "makeMove(txn): Setting firstFlippedCardId = $cardId")
                    currentData.child("firstFlippedCardId").value = cardId
                } else if (room.secondFlippedCardId == null) {
                    android.util.Log.d("FirebaseManager", "makeMove(txn): Setting secondFlippedCardId = $cardId")
                    currentData.child("secondFlippedCardId").value = cardId
                    currentData.child("checkingMatch").value = true
                    currentData.child("lastMovePlayerId").value = playerId
                }
                
                success = true
                return Transaction.success(currentData)
            }
            
            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    android.util.Log.e("FirebaseManager", "makeMove(txn): Error - ${error.message}")
                    continuation.resume(false)
                } else if (!committed) {
                    android.util.Log.w("FirebaseManager", "makeMove(txn): Transaction aborted")
                    continuation.resume(false)
                } else {
                    android.util.Log.d("FirebaseManager", "makeMove(txn): Transaction committed successfully")

                    if (success) {
                        val move = OnlineMove(playerId, cardId)
                        movesRef.child(roomId).push().setValue(move.toMap())
                    }
                    
                    continuation.resume(success)
                }
            }
        })
    }
    
    /**
     * Проверить совпадение и обновить состояние игры
     */
    suspend fun checkMatch(roomId: String, card1Id: Int, card2Id: Int) {
        android.util.Log.d("FirebaseManager", "checkMatch: card1=$card1Id, card2=$card2Id")
        
        val snapshot = roomsRef.child(roomId).get().await()
        
        if (!snapshot.exists()) return
        
        val room = snapshot.getValue(OnlineGameRoom::class.java) ?: return

        // Используем currentPlayerId из комнаты (игрок который ДЕЛАЕТ ход)
        val playerId = room.currentPlayerId
        
        val card1Index = room.cards.indexOfFirst { it.id == card1Id }
        val card2Index = room.cards.indexOfFirst { it.id == card2Id }
        
        if (card1Index == -1 || card2Index == -1) {
            android.util.Log.e("FirebaseManager", "checkMatch: Cards not found")
            return
        }
        
        val card1 = room.cards[card1Index]
        val card2 = room.cards[card2Index]
        
        val isMatch = card1.pairId == card2.pairId
        
        android.util.Log.d("FirebaseManager", "checkMatch: isMatch=$isMatch, pairId1=${card1.pairId}, pairId2=${card2.pairId}, currentPlayer=$playerId")
        
        if (isMatch) {
            // Найдена пара - обновляем карточки и счет
            roomsRef.child(roomId).child("cards").child(card1Index.toString()).updateChildren(
                mapOf(
                    "matched" to true,
                    "matchedBy" to playerId
                )
            ).await()
            
            roomsRef.child(roomId).child("cards").child(card2Index.toString()).updateChildren(
                mapOf(
                    "matched" to true,
                    "matchedBy" to playerId
                )
            ).await()

            // Обновляем счет
            val isHost = room.hostPlayerId == playerId
            val scoreField = if (isHost) "hostPairs" else "guestPairs"
            val currentPairs = if (isHost) room.hostPairs else room.guestPairs
            
            roomsRef.child(roomId).child(scoreField).setValue(currentPairs + 1).await()
            
            android.util.Log.d("FirebaseManager", "checkMatch: Match found! Updated score for $playerId")

            // Проверяем, не закончилась ли игра
            val totalMatched = room.cards.count { it.matched } + 2
            if (totalMatched == room.cards.size) {
                roomsRef.child(roomId).child("gameFinished").setValue(true).await()
            }
            
        } else {
            // Не совпадение - переворачиваем карточки обратно
            android.util.Log.d("FirebaseManager", "checkMatch: No match, flipping cards back")
            
            roomsRef.child(roomId).child("cards").child(card1Index.toString())
                .child("flipped").setValue(false).await()
            
            roomsRef.child(roomId).child("cards").child(card2Index.toString())
                .child("flipped").setValue(false).await()
        }
        
        // ВСЕГДА передаем ход другому игроку после проверки пары (правило игры)
        val nextPlayer = if (playerId == room.hostPlayerId) 
            room.guestPlayerId else room.hostPlayerId
        
        // Проверяем что nextPlayer не null перед передачей хода
        if (nextPlayer != null) {
            roomsRef.child(roomId).child("currentPlayerId").setValue(nextPlayer).await()
            android.util.Log.d("FirebaseManager", "checkMatch: Turn passed from $playerId to $nextPlayer (strict turn order)")
        } else {
            android.util.Log.e("FirebaseManager", "checkMatch: Cannot pass turn - nextPlayer is null!")
        }
        
        // Сбрасываем поля отслеживания открытых карт
        roomsRef.child(roomId).updateChildren(mapOf(
            "firstFlippedCardId" to null,
            "secondFlippedCardId" to null,
            "checkingMatch" to false,
            "lastMovePlayerId" to null
        )).await()
        
        android.util.Log.d("FirebaseManager", "checkMatch: Reset flipped card tracking")
    }
    
    /**
     * Подписка на обновления комнаты
     */
    fun observeRoom(roomId: String): Flow<OnlineGameRoom?> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val room = snapshot.getValue(OnlineGameRoom::class.java)
                trySend(room)
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        roomsRef.child(roomId).addValueEventListener(listener)
        
        awaitClose {
            roomsRef.child(roomId).removeEventListener(listener)
        }
    }
    
    /**
     * Поиск доступных комнат
     */
    suspend fun findAvailableRooms(): List<OnlineGameRoom> {
        android.util.Log.d("FirebaseManager", "findAvailableRooms: Loading rooms...")
        
        val snapshot = roomsRef
            .limitToFirst(50)
            .get()
            .await()
        
        android.util.Log.d("FirebaseManager", "findAvailableRooms: Got ${snapshot.childrenCount} rooms")
        
        val rooms = mutableListOf<OnlineGameRoom>()
        snapshot.children.forEach { child ->
            try {
                val room = child.getValue(OnlineGameRoom::class.java)
                if (room != null && room.guestPlayerId == null && !room.gameStarted) {
                    rooms.add(room)
                    android.util.Log.d("FirebaseManager", "findAvailableRooms: Found room ${room.roomId}")
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseManager", "findAvailableRooms: Error parsing room", e)
            }
        }
        
        android.util.Log.d("FirebaseManager", "findAvailableRooms: Returning ${rooms.size} available rooms")
        return rooms
    }
    
    /**
     * Покинуть комнату
     */
    suspend fun leaveRoom(roomId: String) {
        val playerId = getCurrentPlayerId()
        val snapshot = roomsRef.child(roomId).get().await()
        
        if (!snapshot.exists()) return
        
        val room = snapshot.getValue(OnlineGameRoom::class.java) ?: return
        
        when (playerId) {
            room.hostPlayerId -> {
                roomsRef.child(roomId).removeValue().await()
            }
            room.guestPlayerId -> {
                roomsRef.child(roomId).child("guestPlayerId").removeValue().await()
                if (room.gameStarted) {
                    roomsRef.child(roomId).child("gameFinished").setValue(true).await()
                }
            }
        }
    }
    
    /**
     * Генерация карточек для уровня
     */
    private fun generateCards(level: Int): List<OnlineCard> {
        val pairsCount = when (level) {
            1 -> 4
            2 -> 6
            3 -> 9
            else -> 4
        }
        
        val cards = mutableListOf<OnlineCard>()
        var cardId = 0
        
        for (pairId in 0 until pairsCount) {
            // Создаём две карточки с одинаковым pairId
            cards.add(OnlineCard(cardId++, pairId, pairId))
            cards.add(OnlineCard(cardId++, pairId, pairId))
        }

        return cards.shuffled()
    }
    
    companion object {
        @Volatile
        private var instance: FirebaseGameManager? = null
        
        fun getInstance(): FirebaseGameManager {
            return instance ?: synchronized(this) {
                instance ?: FirebaseGameManager().also { instance = it }
            }
        }
    }
}
