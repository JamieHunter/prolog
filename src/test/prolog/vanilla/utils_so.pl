%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%                                              %
%  Various utility predicates                  %
%  for validation suite.                       %
%                                              %
%  Author  J.P.E. Hodgson                      %
%       Started  19/12/1995                    %
%                                              %
%  Copyright  J.P.E Hodgson                    %
%             Saint Joseph's University        %
%             Philadelphia.   PA 19131         %
%                                              %
%   May be used freely provided                %
%   acknowledgement is made.                   %
%                                              %
%   Thanks to Ken Bowen of ALS for support     %
%                                              %
%   All messages are written to a the standard %
%   output.                                    %
%                                              %
%   This version contains changes by           %
%   Jamie Hunter 2019                          %
%                                              %
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

:- set_prolog_flag(unknown, error).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%  not everybody does flags the same way.
%
%

get_current_prolog_flag(F,V) :-
	current_prolog_flag(F,V).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%	report_list(+Reason, +List)
%
%  write a list of exceptions 
%

report_list(Reason, []) :-
	log( Reason), log_nl.  

report_list(Reason, List) :-
	log(Reason), log_nl,
      pp_list(List).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%	pp_list(+List) pretty print a list
%

pp_list([]) :-log_nl.
pp_list([H|T]) :-
      log( '     '), log(H),log_nl,
      pp_list(T).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%    make_list(+Length, -List)
%
%  make a list of length Length.
%

make_list(Len, List) :-
       make_list(Len, [], List).

make_list(0, L,L).
make_list(N, Sofar, List) :-
     N > 0, !,
     N1 is N - 1,
     make_list(N1, [X| Sofar], List).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%     
%      defined(+Predicate/+Arity)
%
%   in case the predicate does not exist
%   replaces current_predicate/1
%


defined(P/A) :-
	make_list(A, L),
        G =.. [P|L],			% bogus goal
	catch((G;true), B, error_is_not(existence_error,B)).  % (G;true) succeeds if G does
                                                              % not throw an error.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%  error_is_not(+Error, +Thrown)
%  

error_is_not(Error, Thrown) :-
	Thrown =.. [error, StdError | _],
        ( StdError =.. [existence_error |_]  -> fail ; true).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%   format for testing a predicate
%
%
%	test_predicate :- defined(Predicate, Arity),
%                         do_tests.
%	test_predicate :- log('The predicate '), log(Predicate),
%                         log(' with arity '), log(Arity), log_nl,
%                         log('is not supported.'), log_nl, log_nl.
%	


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%	error_test(+Goal, +ExpectedError)
%
%   Calls a copy of Goal and if the ExpectedError
%   is not thrown reports the difference
%
%   ExpectedError contains only the standard required specification
%   so that the thrown error is to be
%       error(ExpectedError, ImpDef).
% 
%   the metacall try(:GCopy, +Goal) reports an unexpected success or failure
%   of a goal that should produce an error.
%


error_test(Goal, ExpectedError) :-
	copy_term(Goal, G),
        copy_term(ExpectedError, E),
	catch(catch(try(G, Goal), error(E,Impdef), true), 
              Thrown, 
              report_difference(Goal, ExpectedError, Thrown)
             ).



try(G, Goal) :-
	call(G),
	    log('[Failed] Unexpected success rather than error in: '),
	    log(Goal),log_nl
        ;
        log('[Failed] Unexpected failure rather than error in: '),
        log(Goal),log_nl.


report_difference(Goal, Expected, Actual):-
	log('Goal: '), log(Goal), log(' attempted.'), 
       log_nl,
        log('Standard Part of Expected Error: '), 
        log( error(Expected,ImpDef)),log_nl,
	log( '[Failed] Error Thrown: '), log( Actual),log_nl,
        log_nl.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%   test_val(+Goal, -Arg, +Expected_Val)
%
%
%	Test the goal G with "output" Arg to check for
%  Expected value

compare_value(A, Expected) :-
    % allow for rounding errors
    float(Expected),
    float(A),
    round(A*10000) =:= round(Expected*10000)
    .

compare_value(A, Expected) :- A = Expected.

test_val(Goal, Arg, Expected) :-
       copy_term(Goal/Arg, G/A), 
	catch( (call(G),
                   ( compare_value(A, Expected) -> true
                    ;
                    log( 'Goal: '), log( Goal),log_nl,
                    log( '[Failed] gave unexpected value: '), log(  A),
                   log_nl,
                    log(' expected value: '), log(Expected),  
                   log_nl
                   )
                 ), 
                 B, 
                report_error(Goal,B)
             ), !.
test_val(Goal, Arg, _) :-
	log( '[Failed] Unexpected failure of the goal '),
        log( Goal),
        log_nl.


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%   
% test_true(+Goal)     % writes if Goal fails
% test_false(+Goal)    % writes if goal succeeds
% 
%

test_true(G) :- copy_term(G,G1), 
                catch(call(G1), 
                      B, 
                      report_error(G,B)
                      ), 
                !.
test_true(G)  :- log( 'Goal: '), log( G),log_nl,
		log( '[Failed] Goal failed when it should have succeeded.'),
               log_nl.

test_false(G) :- copy_term(G,G1), 
                 catch( (call(G1), log( 'Goal:  '), 
                         log(G),log_nl,
	                 log('[Failed] Goal succeeded when it should fail.'),
                        log_nl),
                        B,
                        report_error(G,B)
                      ), 
                 !.
test_false(G).



report_error(G,B) :-
	log( '[Failed] Unexpected error:'), log(B),log_nl,
        log( 'raised by goal: '), log( G), log_nl,
        log( 'that should not raise an error'),
       log_nl, log_nl.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%  do_catch/3
%
%  like catch needed for testing copy_term since
%  test_true etc use copy_term.
%

do_catch(X,Y,Z) :-
	catch(X,Y,Z).

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%  eval_or_fail(+Goal)
%
%  Used to test whether an evaluable functor is
%  supported.
%

eval_or_fail(X) :-
	catch( call(X), _, fail).


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%
% Open logfile  for consistency.
%

start_log.

end_log.

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%
%    Log results to standard out..
%

log(Text) :-
	write(Text).

log_nl :-  nl.
