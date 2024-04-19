# Splitty

This is a group project for the OOPP course at TU Delft CSE bachelors programme. 

It allows users to create shared expenses, keep track of their debts.

It was developed in a group of 5, using agile working methods across timeframe of 9 weeks.

It uses popular Java backend libraries like Springboot, Hibernate, Jakarta. The front-end was developed using JavaFX.

## Instructions on how to execute
#### Server
In the root directory of the project execute:
`./gradlew bootRun`

If you are on Windows, use `gradlew.bat` instead.

Once the server has started up, it will print the administrator password to the console. This can be used to log in to the admin view.

For the demo, we have provided some test data in the database.
#### Client
In the root directory of the project execute:
`./gradlew run`

To run multiple clients, execute the above command in a new terminal window.

If you are on Windows, use `gradlew.bat` instead.

## Instructions on how to access features
When Splitty starts up, you’ll first have to input the URL of the server. You will only have to do this once, the application will verify if the server URL is valid and reachable. You can always change the URL later in the config.json file stored with the application.
The first thing you’ll see is the start screen. It is possible to join or create an event on this screen. You can remove an event from your list of events by pressing on the delete button (or using the shortcut). This’ll only remove the event from your list. To remove an event from the database you must log in as admin.
In an event you can see an overview of all the expenses. To see a summary of what each person owes, click on settle debts. You can simplify the expenses by clicking the settle button on the settle debts page.

### Admin
Whenever the server boots up, an admin password is generated and printed in the terminal where it is booted up. In the apps settings page, you can press on “Admin Login” and there you can fill in the admin password to become an admin.
Admins can see every event, order events how they want, delete events (from the database), export events, and import backed up events from json files.


### Live Language switch
To switch the current language just press on the button in the top right where you can see the current language
To import a new language you can fill in the dictionary in the settings page, and give your new language a title. The new language will be saved locally, and so will your choice of language.

### Detailed expenses
When viewing the event, you can see all the expenses in that event. Here you can:
- See all the expenses in the event
- Filter and sort the expenses on date, tag, involved participants, or amount of money.
- By double clicking on an expense (or using the shortcut) you can see a more detailed overview of the expense. Such as how much money every participant owes to the person who paid. In this screen you can:
- Edit the amount someone already paid by clicking on the field and then editing it in the input field that appears. So you can partially settle some debt.
- Edit the whole expense. So you can change basic info such as the person who paid, the title, or how it was split.
- Remind people who are involved. But more about that under the email section.

When adding a new expense it automatically sets the current date and you can give it basic information such as a title, the participant who paid, and you can fully customize how the amount of money is split between participants. Split equally will split the given amount equally between the chosen participants. If not equally split, you can fill in how much everyone owes separately.

### Foreign currency
In the settings page you can select your preferred currency. Every amount of money in the app will be converted to that currency. The chosen currency will persist if you restart your app. You can also add expenses in a specific currency.

### Event Overview
On this page, you have an overview of all the expenses related to the event. You can sort the expenses based on the amount and the date, and you can filter them based on their tag and the person who paid for it or has to pay their share.
By clicking on an expense once (or navigating to it using keyboard), multiple options about the tag and the color of the tag will appear. Here you can choose a tag, create a new tag or change the color of the tag of any expense.
You can change the name of the event, add participants, add expense, send invites to the event or go to the statistics page via this page

### Statistics
The statistics page shows a pie chart of all the expenses of an event, sorted based on their tag, and colored based on the color you chose for the tag in the event overview page.
In case you’re having any difficulty distinguishing the different colors, you can press on any slice, and all the information related to that slice will be shown to you. You can press on the slice again to make the information disappear. Of course, you still have the option to change the color of the tag to a color you’re more comfortable with in the event overview page.

### Shortcuts
#### All pages:
- Esc to return to the previous page.
- Tab to navigate through the buttons, menus and fields
- Enter to activate a button
#### Start screen:
- S to go to settings
- When on the textfields, ENTER to create or join event
- Enter on an event to go to that event’s page
- DELETE to delete an event (if you have an event selected)
#### Event overview:
- F2 to change event name
- P to add or edit participants
- E to add expense
- D to go to Settle debts page
- S to show statistics of current event expenses
- Enter on an expense to go to that expense’s page
- DELETE to delete an expense (if you have an expense selected
#### Expense view:
- E to edit current expense
#### Settings:
- E to edit credentials
- A to log in as admin
#### Undo & Redo
- Ctrl + z to undo
- Ctrl + y to redo
  - These are all the actions you can undo or redo:
      - create expense
      - delete expense
      - change tag of expense
      - edit expense
      - change paid amount
      - Simplify expenses using settle debts

### Long polling and websocket
The websocket connection is added on the event overview page, all changes on this page are updated via the websocket. The long polling connection is added on the expense view page, here all changes in an expense will be updated live for all users. There is also a long polling connection on the start screen, where admins will be able to see live updates in the events stored in the database,

### Extra features
- The scenes have good color contrast, which helps visually impaired users
- There are multiple keyboard shortcuts for essential features build in
- There are icons to make certain features easier to find
- The navigation is logical, with buttons often being in the same place across scene
- You can navigate between buttons and text fields predictably using tab and enter
- You can undo and redo, this will work multiple times on all action related to expenses
- When something goes wrong, a useful alert pops up
- There are also alerts for confirmations and information about your actions

