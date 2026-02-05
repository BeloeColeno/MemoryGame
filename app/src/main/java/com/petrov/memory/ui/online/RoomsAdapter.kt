package com.petrov.memory.ui.online

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.petrov.memory.databinding.ItemRoomBinding
import com.petrov.memory.domain.model.OnlineGameRoom

/**
 * Адаптер для списка доступных комнат
 */
class RoomsAdapter(
    private val onRoomClick: (OnlineGameRoom) -> Unit
) : ListAdapter<OnlineGameRoom, RoomsAdapter.RoomViewHolder>(RoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemRoomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RoomViewHolder(
        private val binding: ItemRoomBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(room: OnlineGameRoom) {
            binding.apply {
                val levelText = "Уровень ${room.level}"
                val timerText = if (room.timeLimit != null) {
                    "С таймером (${room.timeLimit} сек)"
                } else {
                    "Без таймера"
                }
                
                tvRoomInfo.text = "$levelText • $timerText"
                tvRoomId.text = "ID: ${room.roomId.take(8)}"
                
                root.setOnClickListener {
                    onRoomClick(room)
                }
            }
        }
    }

    class RoomDiffCallback : DiffUtil.ItemCallback<OnlineGameRoom>() {
        override fun areItemsTheSame(oldItem: OnlineGameRoom, newItem: OnlineGameRoom): Boolean {
            return oldItem.roomId == newItem.roomId
        }

        override fun areContentsTheSame(oldItem: OnlineGameRoom, newItem: OnlineGameRoom): Boolean {
            return oldItem == newItem
        }
    }
}
