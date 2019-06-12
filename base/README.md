# CourseApp: Assignment 2

## Authors
* Alex Bondar, 311822258
* Ron Efimov, 308284371

### Previous assignment
This assignment uses the code from the submission by: 311822258-308284371


## Notes

### Implementation Summary
* Data Structures: In this implementation we decided to use Two main data structures.
    The first one is Dictionary, for mapping the between names to indexes and beck,
    names to passwords, users to admins, channels to operators, and so on.
    The second is Avl Tree for managing a sort of all the channels and users
    by the needed sort for statistics, also for handling quick search instead in a list.

* Design:
    the design didn't change much from the previous homework, our usage of CompletableFuture
    was for wrapping mostly.
    we improved the Avl tree implementation and accelerated performance as result.
    message system was added, at the creation of a message time is saved in the storage,
    and there is a mapping of times of user first registration, adding a listener.
    also we mapped the massage itself and parts of it.
    we added pending messages collection in the storage, every user that adding a listener
    check all messages and decide which messages are pending for him/her.
    there is a mapping for message type and more additional information in storage.

* Classes: AppCourseImpl is implementing AppCourse interface.
AppCourseInitializerImpl is implementing AppCourseInitializer interface.
AppCourseStatisticsImpl is implementing AppCourseStatistics interface.
CourseAppModule is for Guice dependency injection.
DataStoreIo class for abstraction layer in the storage path.
RemoteAvlTree class for Avl tree Implementation.
RemoteNode class as a node class in the Avl tree Implementation.
FakeSecureStorage as a Fake implementation of SecureStorage interface, for testing.
MessageImpl is implementing Message interface.
MessageFactoryImpl implementing MessageFactory.

* Class Hierarchy: CourseAppImpl is dependent on: DataStoreIo, RemoteAvlTree, MessageFactoryImpl.
 CourseAppInitializerImpl is dependent on: DataStoreIo.
 CourseAppStatisticsImpl is dependent on: AppCourseImpl, DataStoreIo.
 DataStoreIo is dependent on: SecureStorage(in the tests it is FakeSecureStorage)
 RemoteAvlTree is dependent on: RemoteNode, DataStoreIo.
 RemoteNode is dependent on: DataStoreIo.
 MessageImpl dependent on DataStoreIo



### Testing Summary
standard testing, we made a fake of the SecureStorage for testing all the modules in our project.
In addition we tested the work of the trees by comparing them to standard avl tree(local one).

### Difficulties
There were some difficulties at improving the Avl tree (technical difficulties),
mostly it was hard to understand the demands and restrictions of edge cases.

### Feedback
Post more official responses to conflicts of understanding in the assignment.