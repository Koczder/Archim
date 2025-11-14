package com.demushrenich.archim

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.demushrenich.archim.domain.SortCategory
import com.demushrenich.archim.domain.SortType

object SortingComponent {
    @Composable
    fun GetSortIcon(sortType: SortType, category: SortCategory) {
        val isActive = when (category) {
            SortCategory.NAME -> sortType == SortType.NAME_ASC || sortType == SortType.NAME_DESC
            SortCategory.DATE -> sortType == SortType.DATE_ASC || sortType == SortType.DATE_DESC
            SortCategory.PROGRESS -> TODO()
            SortCategory.LAST_OPENED -> TODO()
        }

        val activeColor = MaterialTheme.colorScheme.primary
        val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant

        Row {
            when (category) {
                SortCategory.NAME -> {
                    Icon(
                        imageVector = Icons.Default.SortByAlpha,
                        contentDescription = stringResource(R.string.sort_by_name),
                        tint = if (isActive) activeColor else inactiveColor
                    )
                    when (sortType) {
                        SortType.NAME_ASC -> Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.sort_ascending),
                            tint = activeColor
                        )
                        SortType.NAME_DESC -> Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = stringResource(R.string.sort_descending),
                            tint = activeColor
                        )
                        else -> {}
                    }
                }

                SortCategory.DATE -> {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = stringResource(R.string.sort_by_date),
                        tint = if (isActive) activeColor else inactiveColor
                    )
                    when (sortType) {
                        SortType.DATE_ASC -> Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = stringResource(R.string.sort_ascending),
                            tint = activeColor
                        )
                        SortType.DATE_DESC -> Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = stringResource(R.string.sort_descending),
                            tint = activeColor
                        )
                        else -> {}
                    }
                }

                SortCategory.PROGRESS -> TODO()
                SortCategory.LAST_OPENED -> TODO()
            }
        }
    }
}