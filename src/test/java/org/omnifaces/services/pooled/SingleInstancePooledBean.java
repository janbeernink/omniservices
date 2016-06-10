/*
 * Copyright 2016 OmniFaces
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.omnifaces.services.pooled;

import java.util.function.Supplier;

@Pooled(destroyOn = RuntimeException.class, dontDestroyOn = IllegalArgumentException.class, maxNumberOfInstances = 1)
public class SingleInstancePooledBean {

	public int getIdentityHashCode() {
		return System.identityHashCode(this);
	}

	public <E extends Exception> void throwException(Supplier<E> exceptionSupplier) throws E {
		throw exceptionSupplier.get();
	}
}