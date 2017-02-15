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
for i = 1:length(peopleInRoom)
    
    % Get randomized indexes
    index = randperm(length(daysInYear));
    
    peopleInRoom(i) = daysInYear(index(1));
end

% Get a list of the unique birthdays
uniqueBirthdays = unique(peopleInRoom);

% Count each unique Birthday for its number of occurences
uniqueBirthdayCount = hist(peopleInRoom, uniqueBirthdays);

% Loop through our unique birthday count, and increment 
% finalSameBirthdayCount for every pair of people with the same birthday
finalSameBirthdayCount = 0;
for i = 1:length(uniqueBirthdayCount)
    % Add the pair(s) to our finalSameBirthday
    if(uniqueBirthdayCount(i) >= 2)
        
        % Adding unique pairs of people using: n(n-1)/2
        % numBirthdayPairs = uniqueBirthdayCount(i) * (uniqueBirthdayCount(i) - 1) / 2;
        % finalSameBirthdayCount = finalSameBirthdayCount + numBirthdayPairs;
        
        % Non unique pairs for the same birthday. Using this as we want 'At least'
        finalSameBirthdayCount = finalSameBirthdayCount + 1;
    end
end

disp('Number of Same Birthdays: ')
disp(finalSameBirthdayCount)
disp('Number of people in the room: ')
disp(numPeople)

probability = finalSameBirthdayCount / numPeople * 100;
fprintf('The probability of a 2 people having the same birthday using this simulation is: %d %%\n', probability)
