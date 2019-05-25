# CourseApp: Assignment 1

## Authors
* Alex Bondar, 311822258
* Ron Efimov, 308284371

### Previous assignment
This assignment uses the code from the submission by: 311822258-308284371

(This is important for people who switched partners, but everyone needs to fill it out)

## Notes

### Implementation Summary

* Implementation summary: 
* Data Structures: In this implementation we decided to use Two main data structures. 
The first one is Dictionary, for mapping the between names to indexes and beck, 
names to passwords, users to admins, channels to operators, and so on. 
The second is Avl Tree for managing a sort of all the channels and users 
by the needed sort for statistics.
 
* Design: We have continued with the design from the previous assignment(HW 0).
In Addition to the previous design, we decided to add Cache in the system for better performance(which in did we got). 
The DataStoreIO, as a layer of abstraction before the SecureStorage, helped as to achieve a fast refactoring and 
made the adding of the Cache very easy. At last we have used Guice for dependency Injection and controlling modules.
 

* Classes: AppCourseImpl is implementing AppCourse interface. 
AppCourseInitializerImpl is implementing AppCourseInitializer interface.
AppCourseStatisticsImpl is implementing AppCourseStatistics interface.
CourseAppModule is for Guice dependency injection.
DataStoreIo class for abstraction layer in the storage path.
RemoteAvlTree class for Avl tree Implementation.
RemoteNode class as a node class in the Avl tree Implementation.
FakeSecureStorage as a Fake implementation of SecureStorage interface, for testing. 

* Class Hierarchy: AppCourseImpl is dependent on: DataStoreIo, RemoteAvlTree.
 AppCourseInitializerImpl is dependent on: DataStoreIo.
 AppCourseStatisticsImpl is dependent on: AppCourseImpl, DataStoreIo.
 DataStoreIo is dependent on: SecureStorage(in the tests it is FakeSecureStorage)
 RemoteAvlTree is dependent on: RemoteNode, DataStoreIo.
 RemoteNode is dependent on: DataStoreIo.
 

### Testing Summary
We made a fake of the SecureStorage for testing all the modules in our project.
In addition we tested the work of the trees by comparing them to standard avl tree(local one).

### Difficulties
There was some difficulties in the implementation for the tree, that is needed to be on the remote storage.

### Feedback
Mainly the internalization and implementation of the tree in the storage, 
it took us couple of days to understand that this is the way you wanted us to implement the assignment.
