@file:Suppress("UnusedReceiverParameter")

package cn.apisium.eim.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun RowScope.Gap(width: Int, modifier: Modifier = Modifier) = Spacer(modifier.width(width.dp))

@Composable
fun ColumnScope.Gap(height: Int, modifier: Modifier = Modifier) = Spacer(modifier.height(height.dp))

@Composable
fun RowScope.Filled(modifier: Modifier = Modifier) = Spacer(modifier.weight(1F))

@Composable
fun ColumnScope.Filled(modifier: Modifier = Modifier) = Spacer(modifier.weight(1F))
