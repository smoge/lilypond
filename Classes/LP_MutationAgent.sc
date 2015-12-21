/* ---------------------------------------------------------------------------------------------------------------
• mutation methods
--------------------------------------------------------------------------------------------------------------- */
+ LP_Selection {
	/* -----------------------------------------------------------------------------------------------------------
	• insert
	----------------------------------------------------------------------------------------------------------- */
	insert { |index, newComponents|
		if (newComponents.isKindOf(Array)) {
			components[0].parent.insertAll(index, newComponents);
		} {
			components[0].parent.insert(index, newComponents);
		};
	}
	/* -----------------------------------------------------------------------------------------------------------
	• remove
	----------------------------------------------------------------------------------------------------------- */
	remove {
		var tieSelection;
		if (components[0].isKindOf(LP_TieSelection)) {
			tieSelection = components[0];
			if (tieSelection.isTieContainer) { tieSelection.root.remove(tieSelection.parent) };
		} {
			//!!! if component.isKindOf(LP_TieSelection) ???
			components.do { |component| component.parent.remove(component) };
		};
	}
	/* -----------------------------------------------------------------------------------------------------------
	• replace
	----------------------------------------------------------------------------------------------------------- */
	replace { |newComponents|
		if (newComponents.isKindOf(Array).not) { newComponents = [newComponents] };
		if (components[0].isKindOf(LP_TieSelection)) { // == LP_TieContainer
			components[0].replace(newComponents);
		} {
			components[0].parent.replaceAll(components[0], newComponents);
			components[1..].do { |component| component.remove };
		};
	}
	/* -----------------------------------------------------------------------------------------------------------
	• noteNames
	----------------------------------------------------------------------------------------------------------- */
	noteNames {
		^components.collect { |component|
			switch(component.class,
				LP_Note, { component.noteName },
				LP_Chord, { component.noteNames },
				LP_TieSelection, { component.noteNames[0] }
			);
		};
	}
	/* -----------------------------------------------------------------------------------------------------------
	• notes
	- get midinotes
	----------------------------------------------------------------------------------------------------------- */
	notes {
		^components.collect { |component|
			switch(component.class,
				LP_Note, { component.note },
				LP_Chord, { component.notes },
				LP_TieSelection, { component.notes[0] }
			);
		};
	}
	/* -----------------------------------------------------------------------------------------------------------
	• noteNames_
	----------------------------------------------------------------------------------------------------------- */
	noteNames_ { |noteNames|
		this.notes_(noteNames);
	}
	/* -----------------------------------------------------------------------------------------------------------
	• notes_
	----------------------------------------------------------------------------------------------------------- */
	notes_ { |notes|
		var noteNames, noteName, newNoteIsChord;

		if (notes.isString) { notes = LP_StringParser(notes) };
		if (notes.size < components.size) { notes = notes.extend(components.size, notes.last) };
		if (notes.size > components.size) { notes = notes[..components.lastIndex] };

		noteNames = notes.collect { |note|
			if (note.isKindOf(Array)) {  LP_NamedChord(note).noteNames } { LP_NamedPitch(note).noteName };
		};

		components.do { |each, i|
			noteName = noteNames[i];
			newNoteIsChord = noteName.isKindOf(Array);
			switch(each.class,
				LP_Note, { this.prNotes_(each, noteName, newNoteIsChord) },
				LP_Chord, { this.prNotes_(each, noteName, newNoteIsChord) },
				LP_TieSelection, { each.components.do { |leaf| this.prNotes_(leaf, noteName, newNoteIsChord) } }
			);
		};
	}
	prNotes_ { |component, noteName, newNoteIsChord|
		switch(component.class,
			LP_Note, {
				if (newNoteIsChord) {
					component.parent.replace(component,
						LP_Chord(noteName, component.preProlatedDuration).cloneAttachmentsFrom(component)
					);
				} { component.note_(noteName) };
			},
			LP_Chord,  {
				if (newNoteIsChord) { component.notes_(noteName) } {
					component.parent.replace(component,
						LP_Note(noteName, component.preProlatedDuration).cloneAttachmentsFrom(component);
					);
				};
			}
		);
	}
	/* -----------------------------------------------------------------------------------------------------------
	• transpose
	----------------------------------------------------------------------------------------------------------- */
	transpose { |intervals|
		if (intervals.isKindOf(Number)) { intervals = [intervals] };
		if (intervals.size < this.size) { intervals = intervals.extend(this.size, intervals.last) };
		this.notes_(components.collect { |each, i| each.note + intervals[i] });
	}
	/* -----------------------------------------------------------------------------------------------------------
	• partition
	----------------------------------------------------------------------------------------------------------- */
	partition { |ratio, notes|
		var firstLeaf, newComponents, tuplet;
		if (this.componentsAreInSameParent) {

			//component = components[0];
			firstLeaf = components[0];
			if (firstLeaf.isKindOf(LP_TieSelection)) { // == LP_TieContainer
				firstLeaf = firstLeaf.components[0].parent;
			};

			tuplet = LP_Tuplet(this.preProlatedDuration, ratio.collect { |dur|
				//!!! shallowClone needed ?? use cloneAttachmentsFrom and deprecate shallowClone ??
				if (dur.isPositive) { firstLeaf.shallowClone.isTiedToNext_(false).preProlatedDuration_(dur) } { LP_Rest(dur) };
			});
			if (notes.notNil) { tuplet.notes_(notes) };
			firstLeaf.parent.replace(firstLeaf, tuplet);
			//!!! tuplet.rewriteTuplet;
		} { error("Can only partition components that share the same parent.") };
	}
	/* -----------------------------------------------------------------------------------------------------------
	• mask
	TODO:
	- add option for rewriting to beatStructure following mask
	----------------------------------------------------------------------------------------------------------- */
	mask { |maskVals|
		var indices, selectionInventory;
		indices =  (0..(maskVals.abs.sum - 1))[..components.lastIndex].clumps(maskVals.abs);
		selectionInventory = this[indices].collect { |each| LP_Selection(each) };
		selectionInventory.do { |sel, i| sel.fuse(maskVals[i].isNegative) };
	}
	/* -----------------------------------------------------------------------------------------------------------
	• fuse
	TODO:
	- perform method only if selection.componentsAreContiguous && selection.componentsAreInSameLogicalVoice ?
	- move method to LP_ContiguousSelection ?
	- add option for rewriting to beatStructure following fuse
	----------------------------------------------------------------------------------------------------------- */
	fuse { |isRest=false|
		var firstLeaf, newLeaf, preProlatedDuration, isLastInSelection;

		if (isRest.not) { this.noteNames_(this.noteNames[0] ! this.size) };
		components = this.clumpByParentage;

		components = components.collect { |clump|
			firstLeaf = clump[0];
			preProlatedDuration = LP_Selection(clump).preProlatedDuration;
			isLastInSelection = (clump == components.last);

			if (firstLeaf.isKindOf(LP_TieSelection)) { // == LP_TieContainer
				firstLeaf.parent.removeAll(firstLeaf.components[1..]);
				firstLeaf = firstLeaf.components[0];
				firstLeaf.parent.isTiedToNext_(isLastInSelection.not);
			};

			newLeaf = if (isRest) { LP_Rest(preProlatedDuration) } {
				firstLeaf.preProlatedDuration_(preProlatedDuration);
			};

			newLeaf.isTiedToNext_(isLastInSelection.not);
			firstLeaf.parent.replace(firstLeaf, newLeaf);
			if (clump.size > 1) { firstLeaf.parent.removeAll(clump[1..]) };
			// if (newLeaf.parent.isKindOf(LP_Tuplet)) { newLeaf.parent.rewrite }; // extract redundant tuplets
			newLeaf;
		};
	}
	/* -----------------------------------------------------------------------------------------------------------
	• beamStructure_
	----------------------------------------------------------------------------------------------------------- */
	beamStructure_ { |beamStructure|
		var selections, spanners;
		components.do { |leaf|
			spanners = leaf.spanners;
			//!!! write a detach method that takes attachment class as an argument
			//!!! e.g. detachByClass(LP_Beam)
			spanners.removeAllSuchThat { |spanner| spanner.isKindOf(LP_Beam) };
		};
		beamStructure = beamStructure.offsets.min(this.size).as(OrderedIdentitySet).asArray.intervals;
		selections = (0..(beamStructure.sum - 1)).clumps(beamStructure);
		selections = selections.collect { |indices| LP_Selection(components[indices]) };
		selections.do { |each|  each.attach(LP_Beam()) };
	}
}
