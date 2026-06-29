document.addEventListener("DOMContentLoaded", function () {
    setupBookingSuccessPopup();
    setupBookingLoadingState();

    const calendarElement = document.getElementById("calendar");

    if (calendarElement) {
        initCalendar(calendarElement);
    }

    loadWeather();
    loadLeaderboard();
});

function setupBookingLoadingState() {
    const form = document.getElementById("booking-form");
    const submitButton = document.getElementById("booking-submit-button");
    const loadingOverlay = document.getElementById("booking-loading-overlay");

    if (!form || !submitButton || !loadingOverlay) {
        return;
    }

    form.addEventListener("submit", function () {
        submitButton.disabled = true;
        submitButton.textContent = "Sender...";

        loadingOverlay.classList.add("is-visible");
    });
}