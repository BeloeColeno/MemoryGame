package com.petrov.memory.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.petrov.memory.domain.model.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

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
        
        // Проверка, что комната не заполнена
        if (room.guestPlayerId != null) return false
        
        // Присоединяемся к комнате
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
        
        // Проверка, что вызывающий - хост
        if (room.hostPlayerId != playerId) return false
        
        // Проверка, что оба игрока присоединились
        if (room.guestPlayerId == null) return false
        
        roomsRef.child(roomId).child("gameStarted").setValue(true).await()
        return true
    }
    
    /**
     * Сделать ход
     */
    suspend fun makeMove(roomId: String, cardId: Int): Boolean {
        val playerId = getCurrentPlayerId()
        val snapshot = roomsRef.child(roomId).get().await()
        
        if (!snapshot.exists()) return false
        
        val room = snapshot.getValue(OnlineGameRoom::class.java) ?: return false
        
        // Проверка, что сейчас ход этого игрока
        if (room.currentPlayerId != playerId) return false
        
        // Проверка, что игра начата и не завершена
        if (!room.gameStarted || room.gameFinished) return false
        
        // Переворачиваем карточку
        val card = room.cards.find { it.id == cardId } ?: return false
        
        if (card.isMatched || card.isFlipped) return false
        
        // Сохраняем ход
        val move = OnlineMove(playerId, cardId)
        movesRef.child(roomId).push().setValue(move.toMap()).await()
        
        // Обновляем состояние карточки
        val cardIndex = room.cards.indexOf(card)
        roomsRef.child(roomId).child("cards").child(cardIndex.toString())
            .child("isFlipped").setValue(true).await()
        
        return true
    }
    
    /**
     * Проверить совпадение и обновить состояние игры
     */
    suspend fun checkMatch(roomId: String, card1Id: Int, card2Id: Int) {
        val playerId = getCurrentPlayerId()
        val snapshot = roomsRef.child(roomId).get().await()
        
        if (!snapshot.exists()) return
        
        val room = snapshot.getValue(OnlineGameRoom::class.java) ?: return
        val card1 = room.cards.find { it.id == card1Id }
        val card2 = room.cards.find { it.id == card2Id }
        
        if (card1 == null || card2 == null) return
        
        val isMatch = card1.pairId == card2.pairId
        
        if (isMatch) {
            // Найдена пара - обновляем карточки и счет
            val card1Index = room.cards.indexOf(card1)
            val card2Index = room.cards.indexOf(card2)
            
            roomsRef.child(roomId).child("cards").child(card1Index.toString()).updateChildren(
                mapOf(
                    "isMatched" to true,
                    "matchedBy" to playerId
                )
            ).await()
            
            roomsRef.child(roomId).child("cards").child(card2Index.toString()).updateChildren(
                mapOf(
                    "isMatched" to true,
                    "matchedBy" to playerId
                )
            ).await()
            
            // Обновляем счет
            val isHost = room.hostPlayerId == playerId
            val scoreField = if (isHost) "hostPairs" else "guestPairs"
            val currentPairs = if (isHost) room.hostPairs else room.guestPairs
            
            roomsRef.child(roomId).child(scoreField).setValue(currentPairs + 1).await()
            
            // Проверяем, не закончилась ли игра
            val totalMatched = room.cards.count { it.isMatched } + 2
            if (totalMatched == room.cards.size) {
                roomsRef.child(roomId).child("gameFinished").setValue(true).await()
            }
            
        } else {
            // Не совпадение - переворачиваем карточки обратно
            val card1Index = room.cards.indexOf(card1)
            val card2Index = room.cards.indexOf(card2)
            
            roomsRef.child(roomId).child("cards").child(card1Index.toString())
                .child("isFlipped").setValue(false).await()
            
            roomsRef.child(roomId).child("cards").child(card2Index.toString())
                .child("isFlipped").setValue(false).await()
            
            // Передаем ход другому игроку
            val nextPlayer = if (playerId == room.hostPlayerId) 
                room.guestPlayerId else room.hostPlayerId
            
            roomsRef.child(roomId).child("currentPlayerId").setValue(nextPlayer).await()
        }
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
                // Показываем только незаполненные комнаты где игра не началась
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
                // Хост покидает - удаляем комнату
                roomsRef.child(roomId).removeValue().await()
            }
            room.guestPlayerId -> {
                // Гость покидает - освобождаем место
                roomsRef.child(roomId).child("guestPlayerId").removeValue().await()
                if (room.gameStarted) {
                    // Если игра началась - завершаем её
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
            // Создаём две карточки с одинаковым imageResId
            val imageResId = pairId // Здесь будет реальный ресурс
            cards.add(OnlineCard(cardId++, imageResId, pairId))
            cards.add(OnlineCard(cardId++, imageResId, pairId))
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
