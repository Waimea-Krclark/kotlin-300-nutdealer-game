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

## Testing Travelling to locations (Valid) (Invalid) (Boundary)

I am going to test the travelling mechanics, which creates an animated interpolation between the current location and the selected one with the user controlling this with buttons. 

### Test Data To Use

Testing player input with the travelling mechanic starting and during the animation, as well as the animation itself and handling the bounds of the x and y pixel coordinates.

### Expected Test Result

The user inputs should be handled properly, starting the animation and doing any update logic, and during the animation, any user inputs should not change anything to do with travelling or locations so that they don't interfere with the systems and break the game.

---

## Testing nut growing functionality (Boundary)(Invalid)

Testing the full mechanics for growing the nuts. The instanced drag and drop functions and the issues with screen boundaries and invalid objects with that, as well as the actual pots and the growth stages and harvesting.

### Test Data To Use

Using the systems to grow nuts, trying multiple different

### Expected Test Result

Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen.

---

## Testing order creation (Invalid) 

Testing old system, laggy and reduces performance, and frame drop on error creation. New system that handles null and invalids.
### Test Data To Use

Details of test data and reasons for selection. Details of test data and reasons for selection. Details of test data and reasons for selection.

### Expected Test Result

Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen.

---

## Testing notoriety bar (Boundary)

Example test description. Example test description. Example test description. Example test description. Example test description. Example test description.

### Test Data To Use

Details of test data and reasons for selection. Details of test data and reasons for selection. Details of test data and reasons for selection.

### Expected Test Result

Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen.

---

## Testing endgame mechanics (Valid, Invalid)

Example test description. Example test description. Example test description. Example test description. Example test description. Example test description.

### Test Data To Use

Details of test data and reasons for selection. Details of test data and reasons for selection. Details of test data and reasons for selection.

### Expected Test Result

Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen.

---

## Testing user inputs during the game (Invalid, Valid)

Example test description. Example test description. Example test description. Example test description. Example test description. Example test description.

### Test Data To Use

Details of test data and reasons for selection. Details of test data and reasons for selection. Details of test data and reasons for selection.

### Expected Test Result

Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen. Statement detailing what should happen.

---

