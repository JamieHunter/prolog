%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%                                              %
%  Section 8.8 of the ISO Prolog Standard      %
%                                              %
%  Tests of clause/2 and current_predicate/1   %
%                                              %
%  Copyright  J.P.E Hodgson                    %
%             Saint Joseph's University        %
%             Philadelphia.   PA 19131         %
%                                              %
%   Thanks to Ken Bowen of ALS for support     %
%   and to Joe Pedano and John Hallat of       %
%   Saint Joseph's for their work on this      %
%   project.                                   %
%   Version for calypso   1 oct 1998           %
%                                              %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%                                              %
%    Database for tests. Taken from 8.8 and    %
%    8.9 of the standard.                      %
%    the standard ISO/IEC 13211-1              %
%                                              %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%





%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

test_clause  :- 
	
	test_true((clause(cat, true))),
	test_true((clause(dog, true))),
	test_val(clause(legs(I,6), Body), Body, insect(I)),
	test_val(clause(legs(C,7), Body1), Body1, (call(C), call(C))),
	test_true(clause(insect(J), T)),
	test_false(clause(x, Body2)).
	

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

test_clause_errors :-
	defined(clause/2),
	error_test(clause(_,B), instantiation_error),
	error_test(clause(4,X), type_error(callable, 4)),
% this should be success?
    test_true(clause(elk(N), Body)),
%	error_test(clause(elk(N), Body),
%		   permission_error(access, private_procedure, elk/1)),
	error_test(clause(atom(_), Body), 
		   permission_error(access, private_procedure, atom/1)).
test_clause_errors :-
         log_nl, log( 'clause/2 not supported.'),
         log_nl, log_nl.


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

test_current_predicate :-
        defined(current_predicate/1),
	test_true(current_predicate(dog/0)),
	test_true(current_predicate(elk/Arity)),
	test_false(current_predicate(foo8/A)),
	test_true(current_predicate(Name/1)),
        test_false(current_predicate(var/2)),
	error_test(current_predicate(4), type_error(predicate_indicator, 4)).

test_current_predicate :-
	log_nl, log( 'current_predicate/1 not supported.'),
	log_nl, log_nl.

test_88 :-

	log('Starting tests for Section 8.8.'), log_nl,
	log_nl, log( 'Testing clause/1.'), log_nl,

	test_clause,

	log_nl, log( 'Testing of clause finished, checking error values.'), 
        log_nl,

	test_clause_errors,

	log_nl, 
        log( 'Testing of error values finished, starting current_predicate tests.'),
        log_nl,
	
	test_current_predicate,

	log( 'Testing completed of current_predicate.'), log_nl,
	log_nl, log( 'All testing completed for Section 8.8.'), 
        log_nl, log_nl, !.
		

