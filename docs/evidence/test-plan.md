# Plan for Testing the Program

The test plan lays out the actions and data I will use to test the functionality of my program.

Terminology:

- **VALID** data values are those that the program expects
- **BOUNDARY** data values are at the limits of the valid range
- **INVALID** data values are those that the program should reject

---

## Testing Bounding Boxes on Locations (Boundary)

I am going to test the world map location selecting, which is done by checking if the mouse is in the location's defined boundary and selecting the clicked on location.

### Test Data To Use

The mouse location on click and the bounds of each location. I will try clicking in a lot of different parts of the map to see the accuracy and consistency.

### Expected Test Result

I expect that when clicking on a location, it will correctly select the right location. If clicking outside of a location it should do nothing, and stay selected on the same location it was on previously.

---

## Testing Travelling to locations (Gameplay, UI, Valid, Invalid, Boundary)

I am going to test the travelling mechanics, which creates an animated interpolation between the current location and the selected one with the user controlling this with buttons. 

### Test Data To Use

Testing player input with the travelling mechanic starting and during the animation, as well as the animation itself and handling the bounds of the x and y pixel coordinates.

### Expected Test Result

The user inputs should be handled properly, starting the animation and doing any update logic, and during the animation, any user inputs should not change anything to do with travelling or locations so that they don't interfere with the systems and break the game.

---

## Testing nut growing functionality (Gameplay, UI, Boundary, Invalid)

Testing the full mechanics for growing the nuts. The instanced drag and drop functions and the issues with screen boundaries and invalid objects with that, as well as the actual pots and the growth stages and harvesting.

### Test Data To Use

Using the systems to grow nuts, trying multiple different interactions to test how the system handles the drag and drop, as well as the mouse interactions.

### Expected Test Result

The drag and drop function should create an object the mouse can drag around, and it should handle it correctly with the mouse position, not throwing errors and instead safely deleting the object on invalid inputs\boundaries. 

---

## Testing order creation (UI, Gameplay, Invalid) 

Testing the order creation, which is a weighted random chance on a timer, that creates an order at a random valid location. This then is added to the players screen along with a timer.


### Test Data To Use

I will let the game create a lot of orders, as well as completing some to test the order creation at different stages of the game, to see how well it makes orders and handles invalids like the nullable current order at locations and the amount of locations avaliable.

### Expected Test Result

Orders should be created at a valid location, setting the current order to be the one created, and should add correctly to the player screen, displaying the correct information without throwing errors. The system should handle the random chances and checks, working properly with invalid results. 

---

## Testing notoriety bar (Boundary)

I will be testing the notoriety bar, which is a custom-made progressbar where the marker interpolates between the two bounds (Min and Max values of the bar)

### Test Data To Use

Running the game, and completing/failing orders to see how the bar handles changes in notoriety, as well as how it handles reaching either boundary.

### Expected Test Result

The bar should reflect the current notoriety, staying in the correct spot for the players current notoriety value, and should not leave the boundaries of the bar, correctly responding to reaching the minimum or maximum boundaries. 

---

## Testing endgame mechanics (Gameplay, UI)

I will be testing both end game states of the game, with the notoriety bar reaching either 0, or 100 which should trigger the game to end, and the player be taken to the end screen.

### Test Data To Use

To test it, achieving both endings of the game to see how well it handles shutting down the game, taking the player to the end screen and handling the audio.

### Expected Test Result

Both the homeless (0 notoriety) and the jail (100 notoriety) endings should correctly trigger. They should stop all gameplay functions like player interactions, end the gameplay timers, and also hide any of the games UI and replace it with the end screen UI. Any remaining audio should be removed and the correct end screen's audio should begin to play. 

---

## Testing user inputs during the game (Invalid, Valid)

I will be testing user inputs during gameplay, how it handles and reacts to certain inputs at certain times, and how it deals with invalid inputs from the player.

### Test Data To Use

Testing all inputs from the player, keyboard input, mouse clicks, mouse position and buttons in the GUI.

### Expected Test Result

User input shouldn't result in unexpected things happening, players should only be able to do valid inputs at the right timer, any invalid inputs should be ignored or handled in a way that isn't destructive to the program and will break the game. 

---

