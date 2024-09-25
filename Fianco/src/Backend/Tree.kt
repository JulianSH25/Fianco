package Backend

data class TreeNode<T>(
    val value: T,
    val children: MutableList<TreeNode<T>> = mutableListOf()
)