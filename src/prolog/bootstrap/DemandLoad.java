// Author: Jamie Hunter, 2019
// Refer to LICENSE.TXT for copyright and license information
//
package prolog.bootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used on Static Field to indicate one or more predicates resolved by demand-loading a resource file.
 * It specifies a resource name, and the static field specifies a list of predicates that are defined by the file.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface DemandLoad {
    /**
     * @return Class resource name.
     */
    String value();
}
