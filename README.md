Overview : Develop an Android application to efficiently load and display images in a scrollable grid without using any third-party image loading libraries.

Features & Requirements

o Image Grid:

Display images in a 3-column square grid.
Center-crop images for a uniform look.

o Image Loading:

Implement asynchronous image loading using the provided API URL.

o Construct image URLs using:

plaintext
imageURL = domain + "/" + basePath + "/0/" + key

o Performance Considerations:

Lazy Loading: Images should load only when needed.
Cancel Unnecessary Requests: If a user scrolls from page 1 to page 10 quickly, image loading from page 1 should be canceled, and page 10 should start loading.
No Scroll Lag: The image grid must remain smooth while scrolling.

o Caching Mechanism:

1. Implement both Memory and Disk cache for efficient image retrieval.
2. If an image is not found in Memory cache, it should be loaded from Disk cache.
3. Once an image is fetched from Disk, update the Memory cache.

o Error Handling:

1.Handle network failures and image load failures gracefully.
2.Display error messages or placeholder images when image loading fails.

o Technology Stack

Language: Java 
Tools: Android Studio (Latest Version)

Installation & Setup

1. Clone the Repository
Open Android Studio and follow these steps:

File > New > Project from Version Control > Select Git as the Version Control System > Paste the GitHub Repository URL in the field provided > Choose the directory where you want to save the project >
Click Clone

GitHub Repository URL >   https://github.com/drstrangesej/AssignmentApp.git

2. Run the App on a Device
Enable Developer Mode & USB Debugging on your phone.
Connect your phone via USB.
Select your device in the Run Configurations.
Click Run to launch the app.
