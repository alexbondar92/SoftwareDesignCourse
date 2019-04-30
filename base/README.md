# CourseApp: Assignment 0

## Authors
* Alex Bondar, 311822258
* Ron Efimov, 308284371

## Notes

### Implementation Summary
We decided to create a system that querying the DB for every operation.
This is made by have a Key to Value kind of a map, like we learn at Data Structure course.
Key: "U$Username", Value: 1 -> logged in; 0 -> logged out; null -> unused.
Key: "P$username$Password", Value: 1 -> password is valid; null -> not valid.
In addition, since the user can have only one session at once, we make the token in "T$username" format.
So we achieved performance of O(1)(1 milisec for the read Op to the server) for every operation of the API.
    
### Testing Summary
We have decided to test our code by mocking the Storage with a mock of it locally with a fake.
Like we learn at the course, we have used the MockK framework for mocking, with a avery/answers concept.
In addition we have tested a randomize of inputs, scaling number of users and more.

### Difficulties
Basicly getting familiar with kotlin and the new technologies like mockK and Gradle.

### Feedback
We will be glad to have a tutorial or workshop or gradle, because it is really important.
  