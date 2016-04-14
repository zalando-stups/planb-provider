/**
 * Please avoid putting @Configuration-annotated classes for test cases under
 * the org.zalando.planb.provider package (or subpackages), because they
 * could mess up the Spring component scan that is executed when importing the Main class.
 *
 * This even applies to inner classes!
 *
 * All extra configs can safely go into this package and should be referenced
 * explicitly in your test cases.
 */
package exclude.from.component.scan;
