package com.selimozturk.monexin.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.TransactionItemRowBinding
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.utils.convertToLongTime

class TransactionAdapter(
    var onItemClicked: ((Transactions) -> Unit) = {},
) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    var items: List<Transactions> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class TransactionViewHolder(private val binding: TransactionItemRowBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(transaction: Transactions) {
            if (transaction.type == "Expense") {
                binding.transactionAmount.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.accent_4
                    )
                )
            } else {
                binding.transactionAmount.setTextColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.accent_3
                    )
                )
            }
            binding.transactionTitle.text = transaction.title
            binding.transactionAmount.text = transaction.amount.toString()
            binding.firsLetterOfTransactionName.text =
                transaction.title.first().toString().uppercase()
            binding.transactionTime.text = transaction.createdAt.convertToLongTime()
            binding.transactionDescription.text = transaction.description
            binding.transactionItemRow.setOnClickListener {
                onItemClicked.invoke(transaction)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding =
            TransactionItemRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

}