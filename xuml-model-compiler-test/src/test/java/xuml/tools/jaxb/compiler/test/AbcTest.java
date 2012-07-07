package xuml.tools.jaxb.compiler.test;

import static org.junit.Assert.assertEquals;
import static xuml.tools.util.database.DerbyUtil.disableDerbyLog;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.junit.Test;

import abc.A;
import abc.A.AId;
import abc.A.BehaviourFactory;
import abc.A.Events.Create;
import abc.A.Events.StateSignature_DoneSomething;
import abc.Context;

public class AbcTest {

	/**
	 * Demonstrates the major aspects of using entities generated by
	 * xuml-model-compiler including reloading signals not fully processed at
	 * time of last shutdown, creation of entities and asynchronous signalling.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testCreateEntityManagerFactoryAndCreateAndSignalEntities()
			throws InterruptedException {

		setup();

		// create the entity manager factory
		EntityManagerFactory emf = Persistence
				.createEntityManagerFactory("abc");

		// pass the EntityManagerFactory to the generated xuml Context
		Context.setEntityManagerFactory(emf);

		// set the behaviour factory for the class A
		A.setBehaviourFactory(createBehaviourFactory());

		// send any signals not processed from last shutdown
		Context.sendSignalsInQueue();

		// create some entities (this happens synchronously)
		A a1 = Context.create(A.class, new A.Events.Create("value1.1",
				"value2.1", "1234"));
		A a2 = Context.create(A.class, new A.Events.Create("value1.2",
				"value2.2", "1234"));
		A a3 = Context.create(A.class, new A.Events.Create("value1.3",
				"value2.3", "1234"));

		// send asynchronous signals to the entities
		a1.signal(new A.Events.SomethingDone("12a"));
		a2.signal(new A.Events.SomethingDone("12b"));

		a3.signal(new A.Events.SomethingDone("12c"));

		// Notice that all the above could be done without explicitly creating
		// EntityManagers at all. Nice!

		// wait a bit for all signals to be processed
		Thread.sleep(5000);

		// // check that the signals had an effect.
		// assertEquals("12a", a1.getAThree());
		// assertEquals("12b", a2.getAThree());
		// assertEquals("12c", a3.getAThree());

		// Just to be sure refresh the entities from the database using the
		// merge method and assert the same
		EntityManager em = emf.createEntityManager();
		// note that the merge method below updates the entity with the latest
		// state from the database using the entity manager em (you could also
		// do em.merge(a1) but you don't get method chaining).
		assertEquals("12a", a1.merge(em).getAThree());
		assertEquals("12b", a2.merge(em).getAThree());
		assertEquals("12c", a3.merge(em).getAThree());
		em.close();

		// shutdown the actor system
		Context.stop();

	}

	private BehaviourFactory createBehaviourFactory() {
		return new A.BehaviourFactory() {
			@Override
			public A.Behaviour create(final A entity) {
				return new A.Behaviour() {

					@Override
					public void onEntryHasStarted(Create event) {
						entity.setId(new AId(event.getATwo(), event.getAOne()));
						entity.setAThree(event.getAccountNumber());
						System.out.println("created");
					}

					@Override
					public void onEntryDoneSomething(
							StateSignature_DoneSomething event) {
						entity.setAThree(event.getTheCount());
						System.out.println(event.getTheCount());
					}
				};
			}
		};
	}

	@Test(expected = NullPointerException.class)
	public void testBehaviourNotSetForAThrowsException() {
		A.setBehaviourFactory(null);
		new A();
	}

	public void setup() {
		disableDerbyLog();
	}

}
