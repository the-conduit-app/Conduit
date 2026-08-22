package com.utilities.conduit.ui

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

object ConduitIcons {
    val Stop: ImageVector
        get() = ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                close()
            }
        }.build()

    val ArrowUpward: ImageVector
        get() = ImageVector.Builder(
            name = "ArrowUpward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 12f)
                lineTo(5.41f, 13.41f)
                lineTo(11f, 7.83f)
                verticalLineTo(20f)
                horizontalLineTo(13f)
                verticalLineTo(7.83f)
                lineTo(18.59f, 13.41f)
                lineTo(20f, 12f)
                lineTo(12f, 4f)
                close()
            }
        }.build()

    val ArrowDropDown: ImageVector
        get() = ImageVector.Builder(
            name = "ArrowDropDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(7f, 10f)
                lineTo(12f, 15f)
                lineTo(17f, 10f)
                close()
            }
        }.build()

    val Person: ImageVector
        get() = ImageVector.Builder(
            name = "Person",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 12f)
                curveTo(14.21f, 12f, 16f, 10.21f, 16f, 8f)
                curveTo(16f, 5.79f, 14.21f, 4f, 12f, 4f)
                curveTo(9.79f, 4f, 8f, 5.79f, 8f, 8f)
                curveTo(8f, 10.21f, 9.79f, 12f, 12f, 12f)
                close()
                moveTo(12f, 14f)
                curveTo(8.13f, 14f, 5f, 15.79f, 5f, 18f)
                verticalLineTo(20f)
                horizontalLineTo(19f)
                verticalLineTo(18f)
                curveTo(19f, 15.79f, 15.87f, 14f, 12f, 14f)
                close()
            }
        }.build()

    val Add: ImageVector
        get() = ImageVector.Builder(
            name = "Add",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 13f)
                horizontalLineTo(13f)
                verticalLineTo(19f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(5f)
                horizontalLineTo(13f)
                verticalLineTo(11f)
                horizontalLineTo(19f)
                close()
            }
        }.build()

    val Menu: ImageVector
        get() = ImageVector.Builder(
            name = "Menu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(3f, 18f)
                horizontalLineTo(21f)
                verticalLineTo(16f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 13f)
                horizontalLineTo(21f)
                verticalLineTo(11f)
                horizontalLineTo(3f)
                close()
                moveTo(3f, 6f)
                verticalLineTo(8f)
                horizontalLineTo(21f)
                verticalLineTo(6f)
                close()
            }
        }.build()

    val CycleBranches: ImageVector
        get() = ImageVector.Builder(
            name = "CycleBranches",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 13f)
                horizontalLineTo(13f)
                verticalLineTo(19f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(11f)
                verticalLineTo(5f)
                horizontalLineTo(13f)
                verticalLineTo(11f)
                horizontalLineTo(19f)
                close()
            }
        }.build()
}
