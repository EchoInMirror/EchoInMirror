@file:Suppress("UNUSED")

package cn.apisium.eim.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Gap(width: Int, modifier: Modifier = Modifier) = Spacer(modifier.width(width.dp))

@Composable
fun Gap(width: Float, modifier: Modifier = Modifier) = Spacer(modifier.width(width.dp))

@Composable
fun RowScope.Filled(modifier: Modifier = Modifier) = Spacer(modifier.weight(1F))

@Composable
fun ColumnScope.Filled(modifier: Modifier = Modifier) = Spacer(modifier.weight(1F))
