package piuk.blockchain.androidcore.data.rxjava

import com.nhaarman.mockito_kotlin.mock
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldHaveKey
import org.amshove.kluent.shouldNotHaveKey
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.RxTest

class RxBusTest : RxTest() {

    private lateinit var subject: RxBus

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()

        subject = RxBus()
    }

    @Test
    fun registerSingleObserver() {
        // Arrange
        val type = String::class.java
        // Act
        subject.register(type)
        // Assert
        subject.subjectsMap shouldHaveKey type
        subject.subjectsMap.size shouldEqual 1
        subject.subjectsMap[type]!!.size shouldEqual 1
    }

    @Test
    fun registerMultipleObserversOfSameType() {
        // Arrange
        val type = String::class.java
        // Act
        subject.register(type)
        subject.register(type)
        subject.register(type)
        // Assert
        subject.subjectsMap shouldHaveKey type
        subject.subjectsMap.size shouldEqual 1
        subject.subjectsMap[type]!!.size shouldEqual 3
    }

    @Test
    fun registerMultipleObserversOfDifferentTypes() {
        // Arrange
        val type0 = String::class.java
        val type1 = Integer::class.java
        val type2 = Double::class.java
        // Act
        subject.register(type0)
        subject.register(type1)
        subject.register(type2)
        // Assert
        subject.subjectsMap shouldHaveKey type0
        subject.subjectsMap shouldHaveKey type1
        subject.subjectsMap shouldHaveKey type2
        subject.subjectsMap.size shouldEqual 3
        subject.subjectsMap[type0]!!.size shouldEqual 1
        subject.subjectsMap[type1]!!.size shouldEqual 1
        subject.subjectsMap[type2]!!.size shouldEqual 1
    }

    @Test
    fun unregisterObserverOneRegistered() {
        // Arrange
        val type = String::class.java
        // Act
        val observable = subject.register(type)
        subject.unregister(type, observable)
        // Assert
        subject.subjectsMap shouldNotHaveKey type
        subject.subjectsMap.size shouldEqual 0
    }

    @Test
    fun unregisterObserverMultipleRegistered() {
        // Arrange
        val type = String::class.java
        // Act
        val observableToBeLeftRegistered = subject.register(type)
        val observableToBeUnregistered = subject.register(type)
        subject.unregister(type, observableToBeUnregistered)
        // Assert
        subject.subjectsMap shouldHaveKey type
        subject.subjectsMap.size shouldEqual 1
        subject.subjectsMap[type]!!.size shouldEqual 1
        subject.subjectsMap[type]!![0] shouldEqual observableToBeLeftRegistered
    }

    @Test
    fun unregisterObserverNoneRegistered() {
        // Arrange
        val type = String::class.java
        // Act
        val observable: Observable<String> = mock()
        subject.unregister(type, observable)
        // Assert
        subject.subjectsMap shouldNotHaveKey type
        subject.subjectsMap.size shouldEqual 0
    }

    @Test
    fun emitEventTypeRegistered() {
        // Arrange
        val type = String::class.java
        val value = "VALUE"
        // Act
        val testObserver = subject.register(type).test()
        subject.emitEvent(type, value)
        // Assert
        testObserver.assertNoErrors()
        testObserver.values().size shouldEqual 1
        testObserver.values()[0] shouldEqual value
    }

    @Test
    fun emitEventTypeNotRegistered() {
        // Arrange
        val typeToRegister = Double::class.java
        val typeToEmit = String::class.java
        val value = "VALUE"
        // Act
        val testObserver = subject.register(typeToRegister).test()
        subject.emitEvent(typeToEmit, value)
        // Assert
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
    }
}