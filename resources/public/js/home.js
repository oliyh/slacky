$(document).ready(
  function() {
    $('#new-account').submit(function(e) {

      e.preventDefault();
      $('#success-message, #failure-message').hide();

      $.ajax({
        url: '/api/account',
        type: 'POST',
        data: {token: $('#token').val(),
               key: $('#key').val()},
        success: function() {
          $('#success-message').css({display: 'inline-block'});
          $('#token, #key').val('');
        },
        error: function() {
          $('#failure-message').css({display: 'inline-block'});
        }
      });
    });

    $('#demo').submit(function(e) {
      e.preventDefault();

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
          console.log('oh noes');
          $('#demo-text').parent('.input-group').addClass('has-error');
          Avgrund.hide();
        }
      });
    });

    $('.example').click(function(e) {
      $('#demo-text').val($(this).data('command'));
    });


    var substringMatcher = function(strs) {
      return function findMatches(q, cb) {
        var matches, substringRegex;

        // an array that will be populated with substring matches
        matches = [];

        // regex used to determine if a string contains the substring `q`
        substrRegex = new RegExp(q, 'i');

        // iterate through the pool of strings and for any string that
        // contains the substring `q`, add it to the `matches` array

        // not sure if a
        // not sure if (.*) or (.*)


        $.each(strs, function(i, str) {
          if (substrRegex.test(str.pattern)) {
            matches.push(str.pattern);
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
      limit: 20
    });
  });
