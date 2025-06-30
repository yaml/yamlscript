document$.subscribe(function () {
  const sidebarControl = document.querySelector(
    ".md-toggle[data-md-toggle='drawer']"
  );
  const sidebarToggleButton = document.querySelector(
    ".md-header__button[for='__drawer']"
  );

  // make interactive elements in the sidebar focusable
  const selectors = ["nav .md-nav__title", "nav .md-nav__link"];
  Array.from(document.querySelectorAll(selectors.join(","))).forEach((el) => {
    el.setAttribute("tabindex", 0);
  });

  // Disable interaction with header, content, and footer when the sidebar
  // drawer is open.
  const onSidebarToggled = (e) => {
    const selectors = [
      "header",
      ".md-content",
      ".md-sidebar[data-md-type='toc']",
      "footer",
    ];
    const isChecked = e.target.checked;
    Array.from(document.querySelectorAll(selectors.join(","))).forEach((el) => {
      if (el) {
        if (isChecked) {
          el.setAttribute("inert", true);
        } else {
          el.removeAttribute("inert");
        }
      }
    });
  };

  // close the sidebar when ESC key is pressed
  const onEscPressed = (e) => {
    const isEscape = e.key === "Escape" || e.key === "Esc";
    if (!isEscape || !sidebarControl.checked) {
      return;
    }

    document.querySelector(".md-overlay[for='__drawer']").click();
    sidebarToggleButton.focus();
  };

  // toggle event listeners for sidebar based on its visibility
  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        document.body.classList.add('tablet');

        sidebarControl.addEventListener("change", onSidebarToggled);
        document.addEventListener("keydown", onEscPressed);
      } else {
        document.body.classList.remove('tablet');

        sidebarControl.removeEventListener("change", onSidebarToggled);
        document.removeEventListener("keydown", onEscPressed);
      }
    });
  });
  observer.observe(sidebarToggleButton);
});