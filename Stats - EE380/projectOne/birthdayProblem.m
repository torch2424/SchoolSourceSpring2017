% 2017 Sprint EE 380
% Project 1
% Aaron Turner
% #011502541

% This function simulates a finding how many people in the same room
% have the same birthday
function birthdayProblem

% Generate our 365 days in a year
daysInYear = 1:365;

% Prompt for the number of people
prompt = 'How many people should be in the room simulation?\n';
numPeople = input(prompt);

% Create a vector of people
peopleInRoom = 1:numPeople;

% Assign birthdays to people randomly
for i = 1:numPeople
    
    % Get randomized indexes
    index = randperm(365);
    
    % Assign the birthday of the ith person to the ith random element
    peopleInRoom(i) = daysInYear(index(1));
end

disp('People in room Birthdays: ');
disp(peopleInRoom)
