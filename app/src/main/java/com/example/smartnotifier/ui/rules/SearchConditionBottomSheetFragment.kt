package com.example.smartnotifier.ui.rules

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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Filter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.smartnotifier.R
import com.example.smartnotifier.core.rule.TitleSearchCondition
import com.example.smartnotifier.databinding.BottomSheetSearchConditionBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout

/**
 * 検索タイトルの条件設定用 BottomSheet。
 *
 * 将来的に AND / OR / NOT 条件の設定 UI をここへ追加する。
 */
class SearchConditionBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSearchConditionBinding? = null
    private val binding get() = _binding!!
    private var condition = TitleSearchCondition()
    private val titleSuggestions: List<String>
        get() = requireArguments().getStringArrayList(ARG_TITLE_SUGGESTIONS).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSearchConditionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        condition = TitleSearchCondition.fromRuleText(
            requireArguments().getString(ARG_SEARCH_TITLE).orEmpty()
        )
        renderConditionChips()

        binding.btnCancelSearchCondition.setOnClickListener {
            dismiss()
        }
        binding.btnAddFilterCondition.setOnClickListener {
            showAddWordDialog(R.string.dlg_title_add_filter_condition) { word ->
                condition = condition.copy(andList = condition.andList + word)
                renderConditionChips()
            }
        }
        binding.btnAddExcludeCondition.setOnClickListener {
            showAddWordDialog(R.string.dlg_title_add_exclude_condition) { word ->
                condition = condition.copy(notList = condition.notList + word)
                renderConditionChips()
            }
        }
        binding.btnAddAnyCondition.setOnClickListener {
            showAddWordDialog(R.string.dlg_title_add_any_condition) { word ->
                condition = condition.copy(orList = condition.orList + word)
                renderConditionChips()
            }
        }
        binding.btnConfirmSearchCondition.setOnClickListener {
            (parentFragment as? Listener)?.onSearchConditionConfirmRequested(
                fragment = this,
                ruleId = requireArguments().getInt(ARG_RULE_ID),
                searchConditionText = condition.toRuleText()
            )
        }
    }

    private fun showAddWordDialog(titleResId: Int, onAdd: (String) -> Unit) {
        val horizontalPadding = (resources.displayMetrics.widthPixels * DIALOG_HORIZONTAL_PADDING_RATIO).toInt()
        val verticalPadding = dpToPx(DIALOG_VERTICAL_PADDING_DP)
        val contentView = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)
        }
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.hint_search_condition_word)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val editText = MaterialAutoCompleteTextView(inputLayout.context).apply {
            setSingleLine(true)
            minHeight = dpToPx(SEARCH_CONDITION_INPUT_MIN_HEIGHT_DP)
            setPadding(
                dpToPx(SEARCH_CONDITION_INPUT_HORIZONTAL_PADDING_DP),
                paddingTop,
                dpToPx(SEARCH_CONDITION_INPUT_HORIZONTAL_PADDING_DP),
                paddingBottom
            )
            threshold = 0
            setAdapter(SearchConditionSuggestionAdapter(titleSuggestions))
            dropDownWidth = dpToPx(SUGGESTION_DROPDOWN_WIDTH_DP)
            dropDownHeight = dpToPx(SUGGESTION_DROPDOWN_HEIGHT_DP)
            setOnClickListener {
                showDropDown()
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    post { showDropDown() }
                }
            }
        }
        inputLayout.addView(editText)
        contentView.addView(inputLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleResId)
            .setView(contentView)
            .setNegativeButton(R.string.btn_cancel, null)
            .setPositiveButton(R.string.captionAddBtn) { _, _ ->
                val word = editText.text?.toString()?.trim().orEmpty()
                if (word.isNotEmpty()) {
                    onAdd(word)
                }
            }
            .show()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private inner class SearchConditionSuggestionAdapter(
        private val items: List<String>
    ) : ArrayAdapter<String>(
        requireContext(),
        android.R.layout.simple_dropdown_item_1line,
        items
    ) {
        private var filteredItems: List<String> = items

        override fun getCount(): Int = filteredItems.size

        override fun getItem(position: Int): String? = filteredItems[position]

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            createSuggestionView(position, convertView, parent)

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View =
            createSuggestionView(position, convertView, parent)

        private fun createSuggestionView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = super.getDropDownView(position, convertView, parent)
            val textView = view as? TextView ?: return view

            textView.minHeight = 0
            textView.includeFontPadding = false
            textView.setPadding(
                dpToPx(SUGGESTION_ROW_HORIZONTAL_PADDING_DP),
                dpToPx(SUGGESTION_ROW_VERTICAL_PADDING_DP),
                dpToPx(SUGGESTION_ROW_HORIZONTAL_PADDING_DP),
                dpToPx(SUGGESTION_ROW_VERTICAL_PADDING_DP)
            )

            return textView
        }

        override fun getFilter(): Filter {
            return object : Filter() {
                override fun performFiltering(constraint: CharSequence?): FilterResults {
                    val query = constraint?.toString().orEmpty().trim()
                    val result = if (query.isEmpty()) {
                        items
                    } else {
                        items.filter { it.contains(query, ignoreCase = true) }
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

    private fun renderConditionChips() {
        renderChipGroup(
            chipGroup = binding.chipGroupFilterCondition,
            words = condition.andList,
            onRemove = { word ->
                condition = condition.copy(andList = condition.andList - word)
                renderConditionChips()
            }
        )
        renderChipGroup(
            chipGroup = binding.chipGroupExcludeCondition,
            words = condition.notList,
            onRemove = { word ->
                condition = condition.copy(notList = condition.notList - word)
                renderConditionChips()
            }
        )
        renderChipGroup(
            chipGroup = binding.chipGroupAnyCondition,
            words = condition.orList,
            onRemove = { word ->
                condition = condition.copy(orList = condition.orList - word)
                renderConditionChips()
            }
        )

        binding.layoutFilterCondition.isVisible = condition.andList.isNotEmpty()
        binding.layoutExcludeCondition.isVisible = condition.notList.isNotEmpty()
        binding.layoutAnyCondition.isVisible = condition.orList.isNotEmpty()
        binding.layoutConditionChips.isVisible =
            condition.andList.isNotEmpty() ||
            condition.notList.isNotEmpty() ||
            condition.orList.isNotEmpty()
    }

    private fun renderChipGroup(
        chipGroup: ChipGroup,
        words: List<String>,
        onRemove: (String) -> Unit
    ) {
        chipGroup.removeAllViews()
        words.forEach { word ->
            val chip = Chip(requireContext()).apply {
                text = word
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    onRemove(word)
                }
            }
            chipGroup.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface Listener {
        fun onSearchConditionConfirmRequested(
            fragment: SearchConditionBottomSheetFragment,
            ruleId: Int,
            searchConditionText: String
        )
    }

    companion object {
        // 追加ダイアログ本文の左右余白。画面幅に対する比率で、0.05f は左右それぞれ5%。
        private const val DIALOG_HORIZONTAL_PADDING_RATIO = 0.05f
        // 追加ダイアログ本文の上下余白。入力欄とボタン領域の窮屈さを調整する。
        private const val DIALOG_VERTICAL_PADDING_DP = 12
        // 条件ワード入力欄の最小高さ。通常の通知タイトル検索欄と近い48dpを基準にする。
        private const val SEARCH_CONDITION_INPUT_MIN_HEIGHT_DP = 48
        // 条件ワード入力欄の左右padding。文字が枠に近すぎる場合はここを増やす。
        private const val SEARCH_CONDITION_INPUT_HORIZONTAL_PADDING_DP = 16
        // 候補リスト全体の最大高さ。大きいほど候補は多く見えるが、キャンセル・追加ボタンを隠しやすい。
        private const val SUGGESTION_DROPDOWN_HEIGHT_DP = 104
        // 候補リストの幅。追加ボタンを隠しすぎないよう入力欄より狭く表示する。
        private const val SUGGESTION_DROPDOWN_WIDTH_DP = 220
        // 候補リスト各行の左右padding。候補文字列の読みやすさを調整する。
        private const val SUGGESTION_ROW_HORIZONTAL_PADDING_DP = 16
        // 候補リスト各行の上下padding。文字の高さ＋少しだけの行高にするため小さめにしている。
        private const val SUGGESTION_ROW_VERTICAL_PADDING_DP = 3
        private const val ARG_RULE_ID = "rule_id"
        private const val ARG_SEARCH_TITLE = "search_title"
        private const val ARG_TITLE_SUGGESTIONS = "title_suggestions"

        fun newInstance(
            ruleId: Int,
            searchTitle: String,
            titleSuggestions: List<String>
        ): SearchConditionBottomSheetFragment =
            SearchConditionBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_RULE_ID, ruleId)
                    putString(ARG_SEARCH_TITLE, searchTitle)
                    putStringArrayList(ARG_TITLE_SUGGESTIONS, ArrayList(titleSuggestions))
                }
            }
    }
}
