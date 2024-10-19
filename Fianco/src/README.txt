# Fianco Game with Advanced AI Techniques

## Overview

Fianco is a strategic board game where two players compete to reach the opponent's baseline or eliminate all opposing pieces. This implementation features an AI opponent enhanced with advanced search algorithms and optimization techniques.

## Features

- **Alpha-Beta Pruning with Negamax Framework**: Efficiently searches the game tree to determine the best move.
- **Adaptive Search Depth (Adaptive Scheme)**: Dynamically adjusts the search depth based on the game state to optimize performance.
- **Multi-Cut Pruning**: Aggressively prunes unpromising branches to reduce the search space.
- **Transposition Tables**: Caches previously evaluated board states to avoid redundant computations.
- **Move Ordering with History Heuristic and Killer Moves**: Prioritizes promising moves to enhance pruning efficiency.
- **Graphical User Interface (GUI)**: Interactive game board with move history, player clocks, and adjustable player types (Human vs. AI).