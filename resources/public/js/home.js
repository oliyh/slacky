$(document).ready(
  function() {
    $('#demo').submit(function(e) {
      e.preventDefault();

      if (window.ga != undefined) {
        ga('send', 'event', 'demo');
      }

      $('#demo-text').parent('.input-group').removeClass('has-error');
      $('#demo-meme').attr('src', '/images/loading.gif').show();
      Avgrund.show('#demo-meme-popup');

      $.ajax({
        url: '/api/meme',
        type: 'POST',
        data: {text: $('#demo-text').val()},
        success: function(r) {
          $('#demo-meme').attr('src', r);
        },
        error: function(r) {
          $('#demo-text').closest('.input-group').addClass('has-error');
          Avgrund.hide();
        }
      });
    });

    $('.example').click(function(e) {
      $('#demo-text').val($(this).data('command'));
    });


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
