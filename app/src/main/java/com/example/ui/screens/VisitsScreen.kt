package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Active", "Completed"

    val visits by viewModel.allVisits.collectAsStateWithLifecycle()
    val entries by viewModel.allEntries.collectAsStateWithLifecycle()

    val filteredVisits = visits.filter { visit ->
        val matchesSearch = visit.customerName.contains(searchQuery, ignoreCase = true) ||
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewVisit,
                containerColor = NavyPrimary,
                contentColor = GoldAccent
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Visit")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF6F8FB))
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Customer, Salesman, or Visit Code") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Active", "Completed").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        color = if (isSelected) NavyPrimary else Color.White,
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) NavyPrimary else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) Color.White else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visits Count
            Text(
                text = "Showing ${filteredVisits.size} Market Trips",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // List
            if (filteredVisits.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No market visits found",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Create a visit to log supplier stops with your retailer.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
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
