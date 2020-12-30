/*
 * Copyright 2020 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.omnifaces.services.pooled;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.jboss.shrinkwrap.api.asset.EmptyAsset.INSTANCE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ArquillianExtension.class)
@DisplayName("PooledContext")
class PooledContextTest {

	@Deployment
	public static Archive<?> createDeployment() {
		return create(WebArchive.class)
				.addAsManifestResource(INSTANCE, "beans.xml")
				.addClasses(Foo.class, FooManager.class)
				.addAsLibraries(create(JavaArchive.class)
						                .addAsManifestResource(INSTANCE, "beans.xml")
						                .addPackages(true, "org.omnifaces.services.pooled")
						                .addPackages(true, "org.omnifaces.services.util")
				)
				.addAsLibraries(Maven.resolver()
				                     .loadPomFromFile("pom.xml")
				                     .resolve("org.omnifaces:omniutils")
				                     .withoutTransitivity()
				                     .asSingleFile());
	}


	private PooledContext pooledContext;

	@Resource
	private BeanManager beanManager;

	private Bean<Foo> fooBean;

	@Inject
	private FooManager fooManager;

	@SuppressWarnings("unchecked")
	@BeforeEach
	private void init() {
		pooledContext = new PooledContext();
		fooBean = (Bean<Foo>) beanManager.getBeans(Foo.class).stream().findAny().get();
	}

	@DisplayName("with empty instance pool and no allocated instance")
	@Nested
	class WithEmptyInstancePoolAndNoAllocatedInstance {

		@DisplayName("indicates that the pooled scope is active")
		@Test
		public void isActive_returnsTrue() {
			assertTrue(pooledContext.isActive());
		}

		@DisplayName("returns null when attempting to get an existing bean instance")
		@Test
		public void get_withoutCreationalContext_returnsNull() {
			Object o = pooledContext.get(fooBean);

			assertNull(o);
		}

		@DisplayName("returns a new instance when attempting to get or create a bean instance")
		@Test
		public void get_withCreationalContext_returnsNewInstance() {
			Foo foo = pooledContext.get(fooBean, beanManager.createCreationalContext(fooBean));

			assertTrue(fooManager.getCreatedInstances().contains(foo));
		}

		@DisplayName("returns false to indicate no instance of a bean has been allocated")
		@Test
		public void hasAllocatedBeanInstance_returnsFalse() {
			boolean hasAllocatedInstanceOfFoo = pooledContext.hasAllocatedInstanceOf(fooBean);

			assertFalse(hasAllocatedInstanceOfFoo);
		}
	}

	static class Foo {
		private final int instance;

		Foo(int instance) {
			this.instance = instance;
		}

		public int getInstance() {
			return instance;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			Foo foo = (Foo) o;
			return instance == foo.instance;
		}

		@Override
		public int hashCode() {
			return Objects.hash(instance);
		}
	}

	@ApplicationScoped
	static class FooManager {

		private ConcurrentSkipListSet<Foo> createdInstances;

		private ConcurrentSkipListSet<Foo> destroyedInstances;

		private AtomicInteger instanceCount = new AtomicInteger(0);

		@Produces
		@Pooled
		Foo createFoo() {
			Foo foo = new Foo(instanceCount.incrementAndGet());

			createdInstances.add(foo);

			return foo;
		}

		void disposeFoo(@Disposes Foo foo) {
			destroyedInstances.add(foo);
		}

		public ConcurrentSkipListSet<Foo> getCreatedInstances() {
			return createdInstances;
		}

		public ConcurrentSkipListSet<Foo> getDestroyedInstances() {
			return destroyedInstances;
		}
	}
}