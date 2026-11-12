/* The Aurelia - small progressive enhancements.
   Every feature here is optional: all forms and actions also work without JavaScript. */
(function () {
    "use strict";

    const ready = (fn) => document.readyState !== "loading" ? fn() : document.addEventListener("DOMContentLoaded", fn);

    /* ---------- broken image fallback ---------- */
    document.addEventListener("error", (event) => {
        const img = event.target;
        if (img && img.tagName === "IMG" && !img.dataset.fallbackApplied) {
            img.dataset.fallbackApplied = "true";
            img.src = "/images/fallback.svg";
        }
    }, true);

    ready(() => {
        /* ---------- navbar: solid background after scrolling ---------- */
        const nav = document.querySelector(".site-nav");
        if (nav) {
            const update = () => nav.classList.toggle("is-scrolled", window.scrollY > 40);
            update();
            window.addEventListener("scroll", update, { passive: true });
        }

        /* ---------- reveal sections on scroll ---------- */
        const revealItems = document.querySelectorAll(".reveal");
        if ("IntersectionObserver" in window && revealItems.length) {
            const observer = new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add("is-visible");
                        observer.unobserve(entry.target);
                    }
                });
            }, { threshold: 0.12 });
            revealItems.forEach((el) => observer.observe(el));
        } else {
            revealItems.forEach((el) => el.classList.add("is-visible"));
        }

        /* ---------- check-in / check-out pairs ---------- */
        const toIso = (d) => {
            const local = new Date(d.getTime() - d.getTimezoneOffset() * 60000);
            return local.toISOString().slice(0, 10);
        };
        const parse = (value) => value ? new Date(value + "T00:00:00") : null;

        document.querySelectorAll("[data-date-range]").forEach((scope) => {
            const checkIn = scope.querySelector("[data-checkin]");
            const checkOut = scope.querySelector("[data-checkout]");
            const nightsOut = scope.querySelectorAll("[data-nights-output]");
            if (!checkIn || !checkOut) return;

            const sync = () => {
                const inDate = parse(checkIn.value);
                if (inDate) {
                    const minOut = new Date(inDate);
                    minOut.setDate(minOut.getDate() + 1);
                    checkOut.min = toIso(minOut);
                    const outDate = parse(checkOut.value);
                    if (!outDate || outDate <= inDate) {
                        checkOut.value = toIso(minOut);
                    }
                }
                const a = parse(checkIn.value), b = parse(checkOut.value);
                const nights = a && b ? Math.round((b - a) / 86400000) : 0;
                nightsOut.forEach((el) => {
                    el.textContent = nights > 0 ? nights + (nights === 1 ? " night" : " nights") : "Select dates";
                });
            };
            checkIn.addEventListener("change", sync);
            checkOut.addEventListener("change", sync);
            if (checkIn.value) sync();
        });

        /* ---------- confirmation dialog for destructive actions ---------- */
        const modalEl = document.getElementById("confirmModal");
        if (modalEl && window.bootstrap) {
            const modal = new bootstrap.Modal(modalEl);
            const msg = modalEl.querySelector("[data-confirm-message]");
            const title = modalEl.querySelector("[data-confirm-title]");
            const okBtn = modalEl.querySelector("[data-confirm-ok]");
            let pendingForm = null;

            document.querySelectorAll("form[data-confirm]").forEach((form) => {
                form.addEventListener("submit", (event) => {
                    if (form.dataset.confirmed === "true") return;
                    event.preventDefault();
                    pendingForm = form;
                    msg.textContent = form.dataset.confirm;
                    title.textContent = form.dataset.confirmTitle || "Please confirm";
                    okBtn.textContent = form.dataset.confirmButton || "Confirm";
                    modal.show();
                });
            });
            okBtn.addEventListener("click", () => {
                if (pendingForm) {
                    pendingForm.dataset.confirmed = "true";
                    modal.hide();
                    pendingForm.submit();
                }
            });
        }

        /* ---------- room form: show fields for the chosen room type ---------- */
        const typeSelect = document.querySelector("[data-type-select]");
        if (typeSelect) {
            const showTypeFields = () => {
                document.querySelectorAll("[data-type-fields]").forEach((group) => {
                    const active = group.dataset.typeFields === typeSelect.value;
                    group.hidden = !active;
                    group.querySelectorAll("input, select").forEach((input) => input.disabled = !active);
                });
            };
            typeSelect.addEventListener("change", showTypeFields);
            showTypeFields();
        }

        /* ---------- image preview in the room form ---------- */
        const imageInput = document.querySelector("[data-image-input]");
        const imagePreview = document.querySelector("[data-image-preview]");
        if (imageInput && imagePreview) {
            const updatePreview = () => {
                imagePreview.dataset.fallbackApplied = "";
                imagePreview.src = imageInput.value.trim() || "/images/fallback.svg";
            };
            imageInput.addEventListener("change", updatePreview);
            imageInput.addEventListener("blur", updatePreview);
            document.querySelectorAll("[data-image-pick]").forEach((btn) => {
                btn.addEventListener("click", () => {
                    imageInput.value = btn.dataset.imagePick;
                    updatePreview();
                });
            });
        }

        /* ---------- admin sidebar on small screens ---------- */
        const sidebar = document.querySelector(".admin-sidebar");
        const backdrop = document.querySelector(".admin-backdrop");
        document.querySelectorAll("[data-sidebar-toggle]").forEach((btn) => {
            btn.addEventListener("click", () => {
                const open = !sidebar.classList.contains("is-open");
                sidebar.classList.toggle("is-open", open);
                backdrop && backdrop.classList.toggle("is-open", open);
                btn.setAttribute("aria-expanded", String(open));
            });
        });
        backdrop && backdrop.addEventListener("click", () => {
            sidebar.classList.remove("is-open");
            backdrop.classList.remove("is-open");
        });

        /* ---------- auto-dismiss toasts ---------- */
        document.querySelectorAll(".toast-luxe").forEach((toast) => {
            const close = () => toast.remove();
            toast.querySelector(".btn-close")?.addEventListener("click", close);
            setTimeout(close, 7000);
        });
    });
})();
