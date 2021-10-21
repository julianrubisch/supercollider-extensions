Delegator {
	var wrapped;

	*new { |wrapped|
		^super.newCopyArgs(wrapped);
	}

	respondsTo { |aSymbol|
		^(super.respondsTo(aSymbol) || wrapped.respondsTo(aSymbol));
	}

	doesNotUnderstand { |selector ... args|
        if(wrapped.respondsTo(selector)) {
			^wrapped.performList(selector, args);
        };
		^this.superPerformList(\doesNotUnderstand, selector, args);
    }
}

AttributesDecorator : Delegator {
	var <attributes;

	*new { |wrapped|
		^super.new(wrapped).init;
	}

	init {
		attributes = IdentityDictionary.new(know: true);
	}

	doesNotUnderstand { |selector ... args|
        if(attributes.respondsTo(selector)) {
			^attributes.performList(selector, args);
        };

		^this.superPerformList(\doesNotUnderstand, selector, args);
	}
}
