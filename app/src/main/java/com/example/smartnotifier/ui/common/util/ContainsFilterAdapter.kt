package com.example.smartnotifier.ui.common.util

/*
 * SmartNotifier-Rev2
 * Copyright (C) 2026  Takeaki Yoshizawa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter

/**
 * 検索タイトル候補のフィルター
 * - 候補を部分一致(contains)で表示するためのフィルター
 *
 * @param context Context
 * @param items 検索候補のリスト
 */
class ContainsFilterAdapter(
    context: Context,
    private val items: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line, items) {

    private var filteredItems: List<String> = items

    override fun getCount(): Int = filteredItems.size

    @Suppress("RedundantNullableReturnType")
    override fun getItem(position: Int): String? = filteredItems[position]

    override fun getFilter(): Filter {
        return object : Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString().orEmpty()

                val result = if (query.isEmpty()) {
                    items
                } else {
                    items.filter {
                        it.contains(query.trim(), ignoreCase = true)
                    }
                }

                return FilterResults().apply {
                    values = result
                    count = result.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredItems = results?.values as? List<String> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }
}
