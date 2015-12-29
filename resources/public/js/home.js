var getUrlParameter = function getUrlParameter(sParam) {
  var sPageURL = decodeURIComponent(window.location.search.substring(1)),
      sURLVariables = sPageURL.split('&'),
      sParameterName,
      i;

  for (i = 0; i < sURLVariables.length; i++) {
    sParameterName = sURLVariables[i].split('=');

    if (sParameterName[0] === sParam) {
      return sParameterName[1] === undefined ? true : sParameterName[1];
    }
  }
};

$(document).ready(
  function() {

    function tokenisedMatches(q, pattern) {
      var queryTokens, patternTokens, match;
      queryTokens = q.trim().split(' ');

      patternTokens = pattern
        .replace(/\[upper\]/g, '(.+)')
        .replace(/\[lower\]/g, '(.+)')
        .replace(/\[search terms or image url\]/g, '(.+)')
        .split(' ')
        .slice(0, queryTokens.length);

      match = true;
      for (i = 0; i < patternTokens.length; i++) {
        if (new RegExp(patternTokens[i], 'i').test(queryTokens[i])
            || new RegExp(queryTokens[i], 'i').test(patternTokens[i])) {
          // continue
        } else {
          match = false;
          break;
        }
      }

      return match;
    }

    var substringMatcher = function(strs) {
      return function findMatches(q, cb) {
        var matches;

        // an array that will be populated with substring matches
        matches = [];

        $.each(strs, function(i, str) {
          if (tokenisedMatches(q, str.pattern)) {
            matches.push(str);
          }

        });

        cb(matches);
      };
    };

    $('#demo-text').typeahead({
      hint: true,
      highlight: true,
      minLength: 0
    },
    {
      name: 'patterns',
      source: substringMatcher(patterns),
      limit: 20,
      display: 'pattern',
      templates: {
        suggestion: function(m) { return '<div class="typeahead-result">'
                                  + '<span>' + m.pattern + '</span>'
                                  + '<div class="hidden-xs">'
                                  + (m.template == undefined ? '' : '<img src="' + m.template + '"/>')
                                  + '</div>'
                                  + '</div>'; }
      }
    });
  });
