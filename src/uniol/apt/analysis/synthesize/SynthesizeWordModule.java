/*-
 * APT - Analysis of Petri Nets and labeled Transition systems
 * Copyright (C) 2014  Uli Schlachter
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package uniol.apt.analysis.synthesize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import uniol.apt.adt.ts.State;
import uniol.apt.adt.ts.TransitionSystem;
import uniol.apt.analysis.language.Word;
import uniol.apt.module.AbstractModule;
import uniol.apt.module.Category;
import uniol.apt.module.ModuleInput;
import uniol.apt.module.ModuleInputSpec;
import uniol.apt.module.ModuleOutput;
import uniol.apt.module.ModuleOutputSpec;
import uniol.apt.module.exception.ModuleException;
import uniol.apt.util.Pair;

/**
 * Provide the net synthesis from a word as a module.
 * @author Uli Schlachter
 */
public class SynthesizeWordModule extends AbstractModule {

	@Override
	public String getShortDescription() {
		return "Synthesize a Petri Net from a word";
	}

	@Override
	public String getName() {
		return "word_synthesize";
	}

	@Override
	public void require(ModuleInputSpec inputSpec) {
		SynthesizeModule.requireCommon(inputSpec);
		inputSpec.addParameter("word", Word.class, "The word that should be synthesized");
	}

	@Override
	public void provide(ModuleOutputSpec outputSpec) {
		SynthesizeModule.provideCommon(outputSpec);
		outputSpec.addReturnValue("separationFailurePoints", String.class);
	}

	static private void appendSeparationFailure(StringBuilder result, Set<String> failures) {
		if (failures.isEmpty())
			return;

		boolean first = true;
		if (result.length() != 0)
			result.append(" ");
		result.append("[");
		for (String event : failures) {
			if (!first)
				result.append(",");
			result.append(event);
			first = false;
		}
		result.append("]");
	}

	@Override
	public void run(ModuleInput input, ModuleOutput output) throws ModuleException {
		Word word = input.getParameter("word", Word.class);
		TransitionSystem ts = makeTS(word);
		SynthesizePN synthesize = SynthesizeModule.runSynthesis(ts, input, output);

		// TODO: Maybe this should be moved from a module into its own class? Or perhaps into a static function?

		if (!synthesize.wasSuccessfullySeparated()) {
			// Since we are looking at finite words, start separation can never fail: Just add a place where
			// every transition removes one token.
			assert synthesize.getFailedStateSeparationProblems().isEmpty();

			// List mapping indices into the word to sets of failed separation problems
			List<Set<String>> failedSeparation = new ArrayList<>(word.size());
			// Add one for the initial state
			failedSeparation.add(new HashSet<String>());
			for (String event : word) {
				failedSeparation.add(new HashSet<String>());
			}

			// Add all failed separation problems into the above list
			for (Pair<String, State> failure : synthesize.getFailedEventStateSeparationProblems()) {
				int index = Integer.parseInt(failure.getSecond().getExtension("index").toString());
				failedSeparation.get(index).add(failure.getFirst());
			}

			// Build the string representation of the separation failures
			StringBuilder result = new StringBuilder();
			for (int index = 0; index < word.size(); index++) {
				if (index != 0)
					result.append(",");
				appendSeparationFailure(result, failedSeparation.get(index));
				if (result.length() != 0)
					result.append(" ");
				result.append(word.get(index));
			}
			appendSeparationFailure(result, failedSeparation.get(word.size()));
			output.setReturnValue("separationFailurePoints", String.class, result.toString());
		}
	}

	@Override
	public Category[] getCategories() {
		return new Category[]{Category.LTS};
	}

	static public TransitionSystem makeTS(List<String> word) {
		TransitionSystem ts = new TransitionSystem();
		State state = ts.createState();
		state.putExtension("index", 0);
		ts.setInitialState(state);

		int index = 1;
		for (String label : word) {
			State nextState = ts.createState();
			nextState.putExtension("index", index);
			ts.createArc(state, nextState, label);

			state = nextState;
			index++;
		}

		return ts;
	}
}

// vim: ft=java:noet:sw=8:sts=8:ts=8:tw=120