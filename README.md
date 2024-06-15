# QLearning Mining Bot

## Overview
This is a Q-Learning-based bot designed for Old School RuneScape (OSRS) that mines iron ore at two different locations and banks the collected ore. The bot uses reinforcement learning principles to optimize its mining activities, deciding which mining area is more valuable based on the success of mining attempts. If it fails to mine an ore (due to another player taking it), the model adjusts its Q-values to determine the most promising mining spot.

## Features
- **Mining at Two Locations**: The bot can mine iron ore at two designated locations.
- **Banking**: After collecting a full inventory of ore, the bot will bank the ores and then return to mining.
- **Reinforcement Learning**: Uses Q-learning to make decisions on which mining spot to use based on previous successes and failures.
- **Adaptive Learning**: Adjusts its strategy based on the availability of ores and the presence of other players.

## How It Works
The bot is implemented using the DreamBot API and Q-learning for decision making. The bot's logic is divided into several key components:

- **States**: Represents different states the bot can be in (e.g., at a mining spot, walking to a mining spot, in the bank).
- **Actions**: Represents different actions the bot can take (e.g., mine ore, walk to spot 1, walk to spot 2, bank ores).
- **Learning Agent**: Manages the Q-table and decides the best action to take based on the current state and learned Q-values.
- **Main Bot Logic**: The main script that integrates all components and runs the bot.

## Files and Their Roles
- **QLearningReinforcementBot.java**: The main script that runs the bot and handles the high-level logic and transitions between states.
- **LearningAgent.java**: Implements the Q-learning algorithm, manages the Q-table, and provides methods to choose actions and update Q-values.
- **State.java**: Enum defining the different states the bot can be in.
- **Action.java**: Enum defining the different actions the bot can take.
