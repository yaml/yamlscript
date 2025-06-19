document$.subscribe(function () {
  const sidebarControl = document.querySelector(
    ".md-toggle[data-md-toggle='drawer']"
  );

  // Disable interaction with header, content, and footer when the sidebar
  // drawer is open.
  sidebarControl.addEventListener("change", (e) => {
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
  });

  // make interactive elements in the sidebar focusable
  const selectors = ["nav .md-nav__title", "nav .md-nav__link"];
  Array.from(document.querySelectorAll(selectors.join(","))).forEach((el) => {
    el.setAttribute("tabindex", 0);
  });
});