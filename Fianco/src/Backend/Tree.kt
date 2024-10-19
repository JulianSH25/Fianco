package Backend

/**
 * Represents a node in a tree structure.
 *
 * @param T The type of the value stored in the node.
 * @property value The value stored in the node.
 * @property children The list of child nodes.
 */
data class TreeNode<T>(
    val value: T,
    val children: MutableList<TreeNode<T>> = mutableListOf()
)