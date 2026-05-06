# Results of Testing

The test results show the actual outcome of the testing, following the [Test Plan](test-plan.md)

---

## Testing Bounding Boxes on Locations (Boundary)

I am going to test the world map location selecting, which is done by checking if the mouse is in the location's defined boundary and selecting the clicked on location.

### Test Data To Use

The mouse location on click and the bounds of each location. I will try clicking in a lot of different parts of the map to see the accuracy and consistency.

### Test Result

![LocationBounds.gif](screenshots/LocationBounds.gif)

The bounds of the location are working as expected, every location is consistently selected when clicked, and clicking outside of the bounds of a location will not incorrectly select another location or throw an error, instead keeping the current location selected and doing nothing.


---

## Testing Travelling to locations (Gameplay, UI, Valid, Invalid, Boundary)

I am going to test the travelling mechanics, which creates an animated interpolation between the current location and the selected one with the user controlling this with buttons.

### Test Data To Use

Testing player input with the travelling mechanic starting and during the animation, as well as the animation itself and handling the bounds of the x and y pixel coordinates.

### Test Result



---

## Testing nut growing functionality (Gameplay, UI, Boundary, Invalid)

Testing the full mechanics for growing the nuts. The instanced drag and drop functions and the issues with screen boundaries and invalid objects with that, as well as the actual pots and the growth stages and harvesting.

### Test Data To Use

Using the systems to grow nuts, trying multiple different interactions to test how the system handles the drag and drop, as well as the mouse interactions.

### Test Result



---

## Testing order creation (UI, Gameplay, Invalid)

Testing the order creation, which is a weighted random chance on a timer, that creates an order at a random valid location. This then is added to the players screen along with a timer.


### Test Data To Use

I will let the game create a lot of orders, as well as completing some to test the order creation at different stages of the game, to see how well it makes orders and handles invalids like the nullable current order at locations and the amount of locations avaliable.

### Test Result

old system, the lag and perfomance issues, new system better performance and better invalid handling.

---

## Testing notoriety bar (Boundary)

I will be testing the notoriety bar, which is a custom-made progressbar where the marker interpolates between the two bounds (Min and Max values of the bar)

### Test Data To Use

Running the game, and completing/failing orders to see how the bar handles changes in notoriety, as well as how it handles reaching either boundary.

### Test Result



---

## Testing endgame mechanics (Gameplay, UI)

I will be testing both end game states of the game, with the notoriety bar reaching either 0, or 100 which should trigger the game to end, and the player be taken to the end screen.

### Test Data To Use

To test it, achieving both endings of the game to see how well it handles shutting down the game, taking the player to the end screen and handling the audio.

### Test Result



---

## Testing user inputs during the game (Invalid, Valid)

I will be testing user inputs during gameplay, how it handles and reacts to certain inputs at certain times, and how it deals with invalid inputs from the player.

### Test Data To Use

Testing all inputs from the player, keyboard input, mouse clicks, mouse position and buttons in the GUI.

### Test Result



---

