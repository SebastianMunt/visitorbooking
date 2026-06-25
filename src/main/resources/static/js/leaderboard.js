function loadLeaderboard() {
    const element = document.getElementById("guest-leaderboard");

    if (!element) {
        return;
    }

    Promise.all([
        fetch("/api/bookings/leaderboard").then(response => response.json()),
        fetch("/api/bookings/guest-highlights").then(response => response.json())
    ])
        .then(([leaderboard, highlights]) => {
            let html = "";

            if (!leaderboard || leaderboard.length === 0) {
                html += `<p>Ingen gæster på leaderboard endnu.</p>`;
            } else {
                html += leaderboard
                    .map((guest, index) => {
                        const bookingText = guest.visitCount === 1 ? "booking" : "bookinger";

                        return `
                            <div class="leaderboard-row">
                                <span class="leaderboard-place">${index + 1}</span>
                                <span class="leaderboard-name">${guest.guestName}</span>
                                <span class="leaderboard-count">${guest.visitCount} ${bookingText}</span>
                            </div>
                        `;
                    })
                    .join("");
            }

            html += `
                <div class="leaderboard-highlights">
                    <div class="leaderboard-highlight">
                        <span>🏆</span>
                        <p>${highlights.longestVisitText}</p>
                    </div>

                    <div class="leaderboard-highlight">
                        <span>🥇</span>
                        <p>${highlights.firstBookingText}</p>
                    </div>

                    <div class="leaderboard-highlight">
                        <span>🧳</span>
                        <p>${highlights.firstVisitText}</p>
                    </div>
                </div>
            `;

            element.innerHTML = html;
        })
        .catch(() => {
            element.textContent = "Kunne ikke hente leaderboard.";
        });
}