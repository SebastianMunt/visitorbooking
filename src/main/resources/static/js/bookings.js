document.addEventListener("DOMContentLoaded", function () {
    setupBookingSuccessPopup();

    const calendarElement = document.getElementById("calendar");

    if (calendarElement) {
        initCalendar(calendarElement);
    }

    loadWeather();
    loadLeaderboard();
});