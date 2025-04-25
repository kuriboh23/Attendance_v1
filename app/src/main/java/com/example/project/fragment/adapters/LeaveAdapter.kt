/*
package com.example.project.fragment.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.project.R
import com.example.project.data.Leave
import com.example.project.data.User

class LeaveAdapter(
    private val onLeaveClick: (Leave) -> Unit
) : RecyclerView.Adapter<LeaveAdapter.LeaveViewHolder>() {

    private var leaveList: List<Leave> = emptyList()
    private var userMap: Map<Long, User> = emptyMap()

    class LeaveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvFullName: TextView = itemView.findViewById(R.id.tvFullName)
        val tvLeaveType: TextView = itemView.findViewById(R.id.tvType)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaveViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leave_request, parent, false)
        return LeaveViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: LeaveViewHolder, position: Int) {
        val leave = leaveList[position]
        val user = userMap[leave.userId] // Get the user from the map
        holder.tvFullName.text = if (user != null) {
            "${user.lastName} ${user.name}"
        } else {
            "Unknown"
        }
        holder.tvLeaveType.text = "${leave.type}"
        holder.tvStatus.text = "${leave.status}"

        val context = holder.itemView.context
        val statusColor = when (leave.status.lowercase()) {
            "approved" -> ContextCompat.getColor(context, android.R.color.holo_green_dark)
            "rejected" -> ContextCompat.getColor(context, android.R.color.holo_red_dark)
            "pending" -> ContextCompat.getColor(context, android.R.color.holo_orange_dark)
            else -> ContextCompat.getColor(context, android.R.color.black)
        }
        holder.tvStatus.setTextColor(statusColor)

        holder.itemView.setOnClickListener {
            onLeaveClick(leave)
        }
    }

    override fun getItemCount(): Int {
        return leaveList.size
    }

    fun setData(newLeaveList: List<Leave>, newUserMap: Map<Long, User>) {
        leaveList = newLeaveList
        userMap = newUserMap
        notifyDataSetChanged()

    }

}*/
