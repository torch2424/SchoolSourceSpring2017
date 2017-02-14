% 2017 Sprint EE 380
% Project 1
% Aaron Turner
% #011502541
% This project simulates a pokerhand. specifically, four of a kind
function fourOfAKind

% To model a deck of cards, we use a MATLAB array

deck = ['AH'; '2H'; '3H'; '4H'; '5H'; '6H'; '7H'; '8H'; '9H'; '10H'; 'JH'; 'QH'; 'KH' ... 
    ;'AS'; '2S'; '3S'; '4S'; '5S'; '6S'; '7S'; '8S'; '9S'; '10S'; 'JS'; 'QS'; 'KS'; ...
    ;'AD'; '2D'; '3D'; '4D'; '5D'; '6D'; '7D'; '8D'; '9D'; '10D'; 'JD'; 'QD'; 'KD'; ...
    ;'AC'; '2C'; '3C'; '4C'; '5C'; '6C'; '7C'; '8C'; '9C'; '10C'; 'JC'; 'QC'; 'KC']

% The number of trials to run
trials = 50

% The accumulator variable records the number of four of a kind hands
k = 0

% Use a loop to obtain multiple hands
for i = 1:trials

    % Get an array of random indexes of our cards
    index = randperm(52)
    
    % Get a hand of cards
    % http://stackoverflow.com/questions/13603713/how-to-select-random-samples-from-a-dataset-in-matlab
    hand = deck(index(1:5), :);
    disp('hand = ')
    disp(hand)
    
    % Sort our hand
    sortedHand = sort(hand)
    
    if (sortedHand(1) == sortedHand(4) || sortedHand(2) == sortedHand(5))
        k = k + 1
    end
end

probability = k / trials
fprintf('The probability of a four of a kind hand using this simulation is: %d', probability)