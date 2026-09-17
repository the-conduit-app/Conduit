package com.utilities.conduit.ui.treeView

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.dk.kuiver.model.Kuiver
import com.dk.kuiver.model.KuiverNode
import com.dk.kuiver.model.buildKuiverWithClassifiedEdges
import kotlin.math.max

internal fun hierarchical(
    kuiver: Kuiver,
    width: Dp,
    height: Dp,
    nodeSpacing: Dp = 20.dp,
    levelSpacing: Dp = 90.dp
): Kuiver {
    if (kuiver.nodes.isEmpty()) return kuiver

    data class TNode(
        val id: String,
        val node: KuiverNode,
        var parent: TNode? = null,
        var children: List<TNode> = emptyList(),

        var x: Float = 0f,
        var depth: Int = 0
    )

    /*
     * Build the strict tree.
     *
     * Conduit is a tree: every node has at most one parent.
     */
    val childrenById = mutableMapOf<String, MutableList<String>>()
    val parentById = mutableMapOf<String, String>()

    kuiver.edges.forEach { edge ->
        if (edge.fromId !in kuiver.nodes || edge.toId !in kuiver.nodes) {
            return@forEach
        }

        if (edge.toId !in parentById) {
            parentById[edge.toId] = edge.fromId
            childrenById
                .getOrPut(edge.fromId) { mutableListOf() }
                .add(edge.toId)
        }
    }

    /*
     * Find the root.
     */
    val rootId =
        kuiver.nodes.keys.firstOrNull { it !in parentById }
            ?: kuiver.nodes.keys.first()

    fun buildTree(
        id: String,
        parent: TNode?,
        depth: Int
    ): TNode {
        val node = TNode(
            id = id,
            node = kuiver.nodes.getValue(id),
            parent = parent,
            depth = depth
        )

        node.children =
            childrenById[id]
                ?.map { childId ->
                    buildTree(
                        id = childId,
                        parent = node,
                        depth = depth + 1
                    )
                }
                ?: emptyList()

        return node
    }

    val root = buildTree(rootId, null, 0)

    fun nodeWidth(node: TNode): Float =
        node.node.dimensions?.width?.value
            ?: 36f

    /*
     * The next available X coordinate while laying out leaves/subtrees.
     */
    var nextX = 0f

    /*
     * Layout a subtree.
     *
     * The critical property here is that a subtree is laid out as one
     * contiguous horizontal block. Once a subtree has been placed, the
     * next subtree starts after it.
     */
    fun layoutSubtree(node: TNode) {
        if (node.children.isEmpty()) {
            node.x = nextX
            nextX += nodeWidth(node) + nodeSpacing.value
            return
        }

        /*
         * Lay out every child completely before positioning this node.
         */
        node.children.forEach { child ->
            layoutSubtree(child)
        }

        /*
         * Single-child case:
         *
         * Put the parent directly over its child.
         *
         * This is important for conversation trees where long chains
         * are common.
         */
        if (node.children.size == 1) {
            node.x = node.children[0].x
        } else {
            /*
             * Multiple children:
             *
             * Center the parent over the complete child span.
             */
            val first = node.children.first().x
            val last = node.children.last().x

            node.x = (first + last) / 2f
        }
    }

    layoutSubtree(root)

    /*
     * The recursive leaf packing above guarantees that every subtree is
     * contiguous. Now determine the actual extents.
     */
    val allNodes = mutableListOf<TNode>()

    fun collect(node: TNode) {
        allNodes += node
        node.children.forEach(::collect)
    }

    collect(root)

    val minX =
        allNodes.minOfOrNull {
            it.x - nodeWidth(it) / 2f
        } ?: 0f

    val maxX =
        allNodes.maxOfOrNull {
            it.x + nodeWidth(it) / 2f
        } ?: 0f

    val minDepth =
        allNodes.minOfOrNull { it.depth } ?: 0

    val maxDepth =
        allNodes.maxOfOrNull { it.depth } ?: 0

    /*
     * Shift the tree so its left edge starts at zero.
     */
    val normalizedWidth = maxX - minX

    /*
     * Vertical spacing is independent of horizontal spacing.
     */
    val treeHeight =
        (maxDepth - minDepth) * levelSpacing.value

    /*
     * Center the entire tree inside the requested viewport.
     *
     * If the tree is larger than the viewport, the offset becomes zero
     * rather than compressing or wrapping the tree.
     */
    val offsetX =
        max(0f, (width.value - normalizedWidth) / 2f)

    val offsetY =
        max(0f, (height.value - treeHeight) / 2f)

    /*
     * Convert to Kuiver nodes.
     */
    val updatedNodes =
        kuiver.nodes.mapValues { (id, originalNode) ->

            val treeNode =
                allNodes.firstOrNull { it.id == id }

            if (treeNode == null) {
                originalNode
            } else {
                val x =
                    treeNode.x -
                            minX +
                            offsetX

                val y = treeNode.depth * levelSpacing.value + offsetY

                originalNode.copy(
                    position = DpOffset(
                        x.dp,
                        y.dp
                    )
                )
            }
        }

    return buildKuiverWithClassifiedEdges(
        nodes = updatedNodes.values,
        originalEdges = kuiver.edges
    )
}
