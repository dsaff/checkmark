Befuzz is a framework for controlled fuzz testing, with an emphasis on allowing
repeatable, behavior-matching fuzz tests.  These are test scenarios in which
a development team wants to assert that two code objects have the same observable
behavior across a wide range of inputs and mutations.  In many cases, this is
useful for asserting that, for example, a newly-written collection class 
maintains the same contract as an existing, well-tested and trusted collection
class, without having to write or duplicate a large number of tests.

In situations in which the two objects cannot be instantiated at the same time
or in the same process, the behavior can be serialized to a file and checked
post-execution.