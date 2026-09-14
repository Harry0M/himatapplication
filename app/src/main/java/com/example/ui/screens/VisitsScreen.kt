package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.VisitEntity
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.HimatViewModel

@Composable
fun VisitsScreen(
    viewModel: HimatViewModel,
    onOpenVisit: (VisitEntity) -> Unit,
    onOpenNewVisit: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Active", "Completed"

    val visits by viewModel.visibleVisits.collectAsStateWithLifecycle()
    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    var isHeaderVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            private var accumulatedDelta = 0f

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    val delta = available.y
                    if (delta < 0) {
                        // Scrolling DOWN (swiping up) -> collapse header
                        if (accumulatedDelta > 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta < -25f) {
                            isHeaderVisible = false
                        }
                    } else if (delta > 0) {
                        // Scrolling UP (pulling down / pushing back) -> expand header
                        if (accumulatedDelta < 0) accumulatedDelta = 0f
                        accumulatedDelta += delta
                        if (accumulatedDelta > 18f) {
                            isHeaderVisible = true
                        }
                    }
                }
                return Offset.Zero
            }
        }
    }

    // Keep header visible at the top of the list or when actively searching
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset, searchQuery, isSearchVisible) {
        if (isSearchVisible || searchQuery.isNotBlank() || (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset <= 15)) {
            isHeaderVisible = true
        }
    }

    val filteredVisits = visits.filter { visit ->
        val matchesSearch = searchQuery.isBlank() ||
                visit.customerName.contains(searchQuery, ignoreCase = true) ||
                visit.employeeName.contains(searchQuery, ignoreCase = true) ||
                visit.visitCode.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Active" -> visit.status.equals("Active", ignoreCase = true)
            "Completed" -> visit.status.equals("Completed", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        containerColor = Color(0xFFF6F8FB),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewVisit,
                containerColor = NavyPrimary,
                contentColor = GoldAccent,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Visit")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .nestedScroll(nestedScrollConnection)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            AnimatedVisibility(
                visible = isHeaderVisible,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                ) + fadeIn(
                    animationSpec = tween(durationMillis = 200)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                ) + fadeOut(
                    animationSpec = tween(durationMillis = 180)
                )
            ) {
                Column {
                    // 1. Top Header (Flat, borderless, matching VisitDetailScreen style)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 0.dp,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Assignment,
                                    contentDescription = null,
                                    tint = NavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Market Visits",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary,
                                letterSpacing = (-0.2).sp
                            )
                            Text(
                                text = "${visits.size} total trips • ${visits.count { it.status.equals("Active", true) }} active",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }

                        // Search Toggle Button in top-right header corner
                        Surface(
                            shape = CircleShape,
                            color = if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color.White,
                            border = BorderStroke(1.dp, if (isSearchVisible || searchQuery.isNotBlank()) NavyPrimary else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    isSearchVisible = !isSearchVisible
                                    if (!isSearchVisible) searchQuery = ""
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isSearchVisible || searchQuery.isNotBlank()) Icons.Default.Clear else Icons.Default.Search,
                                    contentDescription = "Toggle Search",
                                    tint = if (isSearchVisible || searchQuery.isNotBlank()) Color.White else NavyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Quick Action Pill
                        Surface(
                            shape = CircleShape,
                            color = NavyPrimary,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onOpenNewVisit() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    tint = GoldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "New Trip",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldAccent
                                )
                            }
                        }
                    }

                    // 2. Rounded Corner Pill Search Bar (Hidden by default, expands when search button clicked)
                    AnimatedVisibility(
                        visible = isSearchVisible || searchQuery.isNotBlank(),
                        enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(180))
                    ) {
                        Column {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "Search visits, customer, or code...",
                                        fontSize = 13.5.sp,
                                        color = TextSecondary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = NavyPrimary,
                                        modifier = Modifier.size(19.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = CircleShape,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedBorderColor = NavyPrimary,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }

                    // 3. Filter Chips (Rounded pills with counts)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf(
                            "All" to visits.size,
                            "Active" to visits.count { it.status.equals("Active", true) },
                            "Completed" to visits.count { it.status.equals("Completed", true) }
                        )

                        filters.forEach { (filter, count) ->
                            val isSelected = selectedFilter == filter
                            Surface(
                                color = if (isSelected) NavyPrimary else Color.White,
                                shape = CircleShape,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) NavyPrimary else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { selectedFilter = filter }
                            ) {
                                Text(
                                    text = "$filter ($count)",
                                    color = if (isSelected) Color.White else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 4. Visits List
            if (filteredVisits.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "No market visits found" else "No matching visits found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (searchQuery.isBlank()) "Tap '+ New Trip' above to log supplier stops." else "Try searching with a different name or visit code.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredVisits) { visit ->
                        VisitCardItem(
                            visit = visit,
                            entriesCount = entries.count { it.visitId == visit.id },
                            totalPcs = entries.filter { it.visitId == visit.id }.sumOf { it.pieces },
                            onClick = { onOpenVisit(visit) }
                        )
                    }
                }
            }
        }
    }
}
