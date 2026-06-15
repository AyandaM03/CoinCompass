# CoinCompass - Navigate Your Finances 

<img width="475" height="350" alt="ic_logo" src="https://github.com/user-attachments/assets/1e1e6b14-9d32-4269-b088-37bca966f2a4" />


**CoinCompass** is a user-friendly Android application designed to help you take control of your financial journey. Whether you're tracking daily expenses, setting savings goals, or managing spending categories, CoinCompass provides the tools you need to stay on track.

##  Evolution of CoinCompass

This project began as **CoinCompass v1 (app-debug)** and was later redesigned and enhanced into **CoinCompass v2 (CCompass)**.

The goal of Version 2 was to improve usability, introduce income management, provide better navigation, and create a more complete personal finance experience.

---

## Version Comparison

| Feature             | CoinCompass v1 (app-debug) |  CoinCompass v2 (CCompass)           |
| ------------------- | ----------------------------- | -------------------------------------- |
| Monthly Income      | Hardcoded to R25,000          | User-defined income stored in database |
| Income Tracking     | Not available                 | Dedicated Add Income screen            |
| Dashboard Balance   | Based on fixed income         | Real-time calculations                 |
| Navigation          | Quick action buttons only     | Bottom Navigation + Quick Actions      |
| Transaction History | Expenses only                 | Income, Expenses & Goals               |
| Filtering           | Not available                 | 4 filter tabs                          |
| Financial Summary   | Basic                         | Monthly Income & Expense summaries     |
| Income Database     | Not available                 | Dedicated Income table                 |
| User Profile        | No income field               | monthlyIncome field added              |
| Budget Progress     | Fixed R5000 limit             | Dynamic calculation                    |
| Add Income          | Not available                 | Fully implemented                      |
| Database Version    | Version 1                     | Version 2 with migration support       |
| User Experience     | Basic finance tracker         | Complete finance management system     |

---

##  Major Improvements Added In Version 2

### Income Management

A completely new income management system was implemented.

**Added:**

* Add Income screen
* Income source selection
* Monthly income tracking
* Income database table
* Live balance updates

---

###  Transactions Dashboard

The old expense history screen was replaced with a modern transactions hub.

## Calendar page
*Calendar is the timeline <br>
*Filters control the view <br>
*Transaction show categories <br>
*Green + button

<table>
 <tr>
  <td align="center">
 <img width="300"  alt="WhatsApp Image 2026-06-15 at 13 00 14 (4)" src="https://github.com/user-attachments/assets/2ed09a58-1e71-4f79-9125-3c1f6fd229e5" />

</td>
<td align="center">
 <img width="300" alt="WhatsApp Image 2026-06-15 at 13 00 18 (1)" src="https://github.com/user-attachments/assets/a9ff3a72-f46e-4924-a078-7afbb431991b" />

</td>
</tr>
</table>


**Analytics:**

* Time filter 
* category chart
* Quick summary box :
* Top Category 
* Total Spending

---
<table>
  <tr>
  <td align="center">
 <img width="300"  alt="WhatsApp Image 2026-06-15 at 13 00 18" src="https://github.com/user-attachments/assets/12b8fb5b-8703-4378-adac-8c400cc8d456" />

</td>
</table>

**Gamification:**

* More Options menu
  *  ├── Transction Calender
  *  ├── Expense History
  * ├── Rewards and Challenges 
  * └── Savings Goals
  
* Rewards and Challenges:
  *  ├── play challenges 
  *  ├── Challenges completed ad saving score
  *  ├── level progress bar
  *  ├── Coin Catch Challenge 
  *  └── Unlocked Badges
  
* Game over screen:
  *  ├── final Score
  *  ├── Reward Points 
  *  └── plat Again or Return to Dashboard

<table>
 <tr>
  <td align="center">
<img width="300"  alt="WhatsApp Image 2026-06-15 at 16 16 32 (1)" src="https://github.com/user-attachments/assets/715e01da-3297-44d7-9d6e-44923f42c9cb" />



</td>
<td align="center">
<img width="300"  alt="WhatsApp Image 2026-06-15 at 16 34 31" src="https://github.com/user-attachments/assets/f95e10e9-8b8c-4b23-b45f-0f3ad02e1f2e" />


</td>

<td align="center">
<img width="300"  alt="WhatsApp Image 2026-06-15 at 16 34 32 (1)" src="https://github.com/user-attachments/assets/0d777893-a944-41d4-b9b2-c25e23f59f69" />


</td>
</tr>
</table>
---

**New Features:**

* All Transactions tab
* Income tab
* Expenses tab
* Goals tab
* Monthly summary cards
* Transaction badges
* Colour-coded transaction types
* Calender
* smart Financial tips

---

###  Improved Navigation

#### Version 1

```text
Dashboard
 ├── Add Expense
 ├── Categories
 ├── Goals
 └── History
```

#### Version 2

```text
Bottom Navigation
 ├── Home
 ├── Budgets
 ├── Analytics
 ├── Savings
 ├── Transactions
 ├── More
     ├──Transactions calender
     ├──Expense History
     ├── Rewards and Challenges
     └── Savings goal

```
<table>
 <tr>
  <td align="center">
<img width="300"  alt="WhatsApp Image 2026-06-15 at 16 16 31" src="https://github.com/user-attachments/assets/190e334a-9f26-42bc-9947-5b35b21c0c4a" />

</td>
<td align="center">
 <img width="300"  alt="WhatsApp Image 2026-06-15 at 16 16 32" src="https://github.com/user-attachments/assets/f3c5512e-0456-4e2b-85de-43920ebae1af" />


</td>
</tr>
</table>

---

###  Database Improvements

#### Version 1

```text
Users
Categories
Expenses
Goals
```

#### Version 2

```text
Users
Categories
Expenses
Goals
Income ← NEW
```

Additional enhancement:

```kotlin
monthlyIncome: Double = 0.0
```

added to the User entity.

---

## Pictures Before vs After

<table>
<tr>
<th>Version 1 (app-debug)</th>
<th>Version 2 (CCompass)</th>
</tr>

<tr>
  <td align="center">
 <img width="300"  alt="image" src="https://github.com/user-attachments/assets/6edb39d9-c435-4824-9556-8f46c222bc71" />
<br>
splash page v1
</td>
<td align="center">
 <img width="300"  alt="WhatsApp Image 2026-06-15 at 13 00 13" src="https://github.com/user-attachments/assets/46471620-f3c7-4d89-ac2d-784f94534968" />

<br>
improved splash
</td>
</tr>

<tr>
  <td align="center">
 <img width="300"  alt="image" src="https://github.com/user-attachments/assets/77f99f7e-cdea-4428-a412-9c41279b81e5" />
<br>
login page v1
</td>
<td align="center">
 <img width="300"  alt="WhatsApp Image 2026-06-15 at 13 00 14" src="https://github.com/user-attachments/assets/8df6bcb2-2e80-450b-9700-f139aa1af020" />
<br>
improved login
</td>
</tr>

<tr>
  <td align="center">

<br>
Registration page v1
</td>
<td align="center">
<img width="300" alt="WhatsApp Image 2026-06-15 at 13 00 14 (1)" src="https://github.com/user-attachments/assets/07c1999c-74fc-45b0-aaa8-80e6616eb70d" />

<br>
improved registration
</td>
</tr>

<tr>
<td align="center">
 <img width="300" alt="image" src="https://github.com/user-attachments/assets/aabf051c-1fa3-4de9-83bc-dd475b9e6c0d" />
<br>
Basic Dashboard
  
</td>

<td align="center">
<img width="300"  alt="WhatsApp Image 2026-06-15 at 13 00 14 (6)" src="https://github.com/user-attachments/assets/2e737edd-2e78-4f83-829e-43fd9123b5cf" />
<br>
Enhanced Dashboard
</td>
</tr>

<tr>
  <td align="center">
<img width="300"  alt="image" src="https://github.com/user-attachments/assets/8cc4db65-8154-4f2a-bd6d-bfbed716fabe" />
<br>
Financial goals v1
</td>
<td align="center">
<br>
<img width="300"  alt="WhatsApp Image 2026-06-15 at 13 00 16 (2)" src="https://github.com/user-attachments/assets/d2b5814f-d930-40c4-bee1-67c06fe9ebe0" />

improved Financial goals
</td>
</tr>

<tr>
<td align="center">
<img width="300"  alt="image" src="https://github.com/user-attachments/assets/f4860ce8-f876-4943-abec-5fb47e7318e3" />
  <br/>
Expense History
</td>

<td align="center">
<img width="300"  alt="WhatsApp Image 2026-06-15 at 13 00 17" src="https://github.com/user-attachments/assets/fac07800-decd-43b2-8340-bceadaeb20b5" />
<br>
Transactions Hub
</td>
</tr>

<tr>
<td align="center">
❌ No Income Tracking
</td>

<td align="center">
  <img width="300" alt="WhatsApp Image 2026-06-15 at 13 00 15" src="https://github.com/user-attachments/assets/20fe95d8-f629-46fe-8c9a-135fc785293c" />
<br/>
✅ Income Tracking
</td>
</tr>

</table>

---

##  Project Outcome

CoinCompass evolved from a basic expense tracker into a complete personal finance management application.

### Results Achieved

✅ Income tracking implemented

✅ Real-time balance calculations

✅ Improved navigation

✅ Better database architecture

✅ Enhanced user experience

✅ Scalable design for future development

✅ More professional and modern UI



## Tech Stack

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Framework**: XML Layouts with [Material Components](https://material.io/develop/android)
*   **Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room) (SQLite)
*   **Architecture**: MVVM (Model-View-ViewModel) logic with LiveData and Coroutines
*   **Binding**: ViewBinding for efficient UI interaction


##  About the Project

Clean programming and user-centric design were priorities in the construction of this project. To aid developers and students in understanding how Room databases, RecyclerView adapters, and contemporary Android UI ideas are implemented, the code is well-commented.  
Made with passion to assist you in managing your money.

##  Video Demonstration

YouTube Link: https://youtube.com/shorts/z0DBXnjCBFE?si=W9KdymYNWDlwDhU6

##  References:

GitHub (2025) GitHub Actions Documentation. Available at: https://docs.github.com/en/actions
(Accessed: 28 April 2026).

GitHub (2025) Building and testing Java with Gradle. Available at: https://docs.github.com/en/actions/automating-builds-and-tests/building-and-testing-java-with-gradle (Accessed: 2 April 2026).

Gradle Inc. (2025) Gradle User Manual. Available at: https://docs.gradle.org
(Accessed: 20 April 2026).

Android Developers (2025) Build your app from the command line. Available at: https://developer.android.com/studio/build/building-cmdline
(Accessed: 20 April 2026).

Android Developers (2025) Android App Bundles. Available at: https://developer.android.com/guide/app-bundle
(Accessed: 20 April 2026).

OpenJDK (2025) JDK Installation Guide. Available at: https://openjdk.org
(Accessed: 20 April 2026).

Git (2025) Git Documentation. Available at: https://git-scm.com/docs
(Accessed: 28 April 2026).

PDFSimpli (2026) Free Text to Speech Tool. Available at: https://pdfsimpli.com/lp/text-to-speech/?uh=Free%20Text%20to%20Speech&account=242-758-0902&utm_source=google&utm_medium=sem&utm_campaign=23663256363&utm_term=free%20text%20to%20speech&network=g&device=m&adposition=&adgroupid=195118460795&placement=&location=1028995&gad_source=1&gad_campaignid=23663256363&gclid=CjwKCAjwtcHPBhADEiwAWo3sJvUwKF66zh7VoH9qiwHGzWXQv46omaWkCe8dHgvudk9r-M0YXGZ-PBoC_W8QAvD_BwE
(Accessed: 28 April 2026).
