function showResults(response) {
  // Clear the existing content of the data container
  $("#dataContainer").empty();
  $("#num-results").empty();
  console.log("show results");

  //test section
  var container = $("<div>Hi</div>");
  var url = "https://www.google.com";
  for (var j = 1; j <= 3; j++) {
    var title = "Page Titties" + j;
    entrycontainer = $(`<div>${title}</div>`);
    var pageUrl = $(`<div><a href=${url}>${url}</a></div>`);
    var lastModifiedDate = "12 April 24";
    sizeofPage = 1024;
    var lastModnSize = $(
      `<div> Last Modified: ${lastModifiedDate}, Size of Page: ${sizeofPage}kb </div>`
    );
    entrycontainer.append(lastModnSize);
    entrycontainer.append([pageUrl]);
    var keywords = $(`<div>`);
    var keywordCount = 3;
    for (var i = 0; i < keywordCount; i++) {
      keywords.append("keyword" + i + ": " + i + ",");
    }
    keywords.append(`</div>`);
    entrycontainer.append(keywords);
    var childlinks = 3;
    childUrl = ["child1", "child2", "child3"];
    for (var i = 0; i < childlinks; i++) {
      var childlink = $(`<div>${childUrl[i]}</div>`);
      entrycontainer.append(childlink);
    }
    container.append(entrycontainer);
  }
  $("#searchResults").append(container);
}

// Attach event handler for "show more" button
$(document).on("click", ".show-more-btn", function () {
  // Toggle visibility of extra links
  $(this).siblings(".child-link:nth-child(n+6)").toggle();

  // Update button text based on visibility
  $(this).text($(this).text() === "Show More" ? "Show Less" : "Show More");
});
