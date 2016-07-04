# Mapping
A mapping specifies which Java types are to be associated with skill
types in order to serialise them. Usually the Java code base is much
larger than the set of typs one wants to serialise. Thus, by specifying
a mapping, the user can pick types they want to serialise. We
distinguish the following cases:

## Implicit mapping
In this case the user only specifies a mapping by naming a skill type
and a (fully qualified) Java type name. All fields will be mapped by
name, that is, fields with equal names are mapped to one another. Fields
that do not have an equivalent are ignored.

## Implicit total mapping
This behaves exactly as an implicit mapping with the difference that
fields without equivalent on the other side will cause an error.

## Explicit mapping
All is named explicitly, type names and field names are mapped by
specifying the mapping.

## Unbound mapping
This "mapping" only points at a Java type and tells skill to introduce a
new skill type for it. The result is a serialisable Java type plus a new
equivalent type in the skill context (which can be used to generate
language bindings for other languages). The mapping must contain a full
list of the wanted fields.

## Unbound total mapping
Just like unbound mapping but all fields are used instead of an explicit
list.


# Mapping file

The mapping file is used to specify the desired mapping. The following
concepts are available. All directives are either using a block syntax
with { } or are terminated using a semicolon.

## Implicit mapping
One can specify an implicit type mapping by using the implicit keyword:

	implicit mySkillType -> com.example.MyJavaType;

## Implicit total mapping
To make the implicit mapping total, use the 'total' keyword instead:

	total mySkillType -> my.ecactly.matching.JavaType;

## Explicit mapping
Explicit mappings are specified by the 'map' keyword followed by a list
of field mappings:

	map mySkillType -> com.example.MyJavaType
	{
		fieldX -> fieldX;
		x -> a;
		y -> b;
	}

## Unbound mapping
Unbound mappings are introduced using the 'new' keyword followed by a
field list:

	new com.example.SomeType {
		herp,
		derp
	}

	new com.example.SomeOtherType {
		duration,
		price,
		distance
	}

## Unbound total mapping
Unbound total mappings are specified using the total' keyword without a
mapping afterwards:

	total com.example.SomeType;
