package com.selimozturk.monexin.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.TransactionItemRowBinding
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.utils.convertToLongTime

class TransactionAdapter(var onItemClicked: (Transactions) -> Unit = {}) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    var items: List<Transactions> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class TransactionViewHolder(private val binding: TransactionItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transactions) = with(binding) {
            transactionTitle.text = transaction.title
            transactionAmount.text = transaction.amount.toString()
            firsLetterOfTransactionName.text = transaction.title.first().toString().uppercase()
            transactionTime.text = transaction.createdAt.convertToLongTime()
            transactionDescription.text = transaction.description

            val colorRes = if (transaction.type == "expense") {
                R.color.accent_4
            } else {
                R.color.accent_3
            }
            transactionAmount.setTextColor(ContextCompat.getColor(root.context, colorRes))

            transactionItemRow.setOnClickListener {
                onItemClicked(transaction)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = TransactionItemRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}
